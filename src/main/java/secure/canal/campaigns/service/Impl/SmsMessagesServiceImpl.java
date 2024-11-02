package secure.canal.campaigns.service.Impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.Campaigns;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.enumeration.MessageStatus;
import secure.canal.campaigns.exception.ApiException;
import secure.canal.campaigns.payload.*;
import secure.canal.campaigns.repository.CampaignsRepository;
import secure.canal.campaigns.repository.SmsMessagesRepository;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.service.EmailService;
import secure.canal.campaigns.service.OrangeSmsService;
import secure.canal.campaigns.service.SmsMessagesService;
import secure.canal.campaigns.stats.Stats;
import secure.canal.campaigns.utils.ExcelUtils;
import secure.canal.campaigns.utils.FileFactory;
import secure.canal.campaigns.utils.ImportConfig;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.springframework.data.domain.PageRequest.of;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SmsMessagesServiceImpl implements SmsMessagesService {

    private final SmsMessagesRepository smsMessagesRepository;
    private final ModelMapper mapper;
    private final UserRepository userRepository;
    private final CampaignsRepository campaignsRepository;
    private final EmailService emailService;
    private final OrangeSmsService orangeSmsService;

    @Override
    public SmsMessagesDto create(SmsMessagesDto smsMessagesDto) {
        SmsMessages smsMessage =mapper.map(smsMessagesDto,SmsMessages.class);
        // get Campaign
        Campaigns campaigns = campaignsRepository.getReferenceById(smsMessagesDto.getCampaign_id());
        // get User
        User users = userRepository.getReferenceById(smsMessagesDto.getId_user());
        // Set initial message details
        smsMessage.setCampaigns(campaigns);
        smsMessage.setUser(users);
        smsMessage.setSentAt(LocalDateTime.now());
        smsMessage.setStatus(MessageStatus.PENDING.toString());
        // Save the message to the database
        SmsMessages savedMessage = smsMessagesRepository.save(smsMessage);

        // Asynchronously send the message after saving
        sendMessageAsync(savedMessage);
        return mapper.map(savedMessage, SmsMessagesDto.class);


//        smsMessages.setCampaigns(campaigns);
//        smsMessages.setUser(users);
//        smsMessages.setSentAt(LocalDateTime.now());
//        smsMessages.setStatus(MessageStatus.SENT.toString());
//        SmsMessages newSmsMessage = smsMessagesRepository.save(smsMessages);
//        emailService.sendSMS(smsMessagesDto.getRecipientNumber(), smsMessagesDto.getMessage());
//        return mapper.map(newSmsMessage,SmsMessagesDto.class);
    }

    // Asynchronous message sending
    private void sendMessageAsync(SmsMessages message) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean isValidNumber = isValidNineDigitNumber(message.getRecipientNumber());
                if (isValidNumber) {
                    TokenResponse tokenResponse = orangeSmsService.getOAuthToken();
                    if (tokenResponse.getStatus() == 200) {
                        // Send the SMS
                        orangeSmsService.sendSms(tokenResponse.getToken(), message.getRecipientNumber(), "GUIDIPRESS", message.getMessage());
                        message.setStatus(MessageStatus.SENT.toString());
                        log.info("SMS sent to {}", message.getRecipientNumber());
                    } else {
                        message.setStatus(MessageStatus.FAILED.toString());
                        log.warn("Failed to get OAuth token for SMS sending");
                    }
                } else {
                    message.setStatus(MessageStatus.FAILED.toString());
                    log.warn("Invalid recipient number: {}", message.getRecipientNumber());
                }
            } catch (Exception e) {
                message.setStatus(MessageStatus.FAILED.toString());
                log.error("Error sending SMS: ", e);
            } finally {
                smsMessagesRepository.save(message);
            }
        }).exceptionally(ex -> {
            log.error("Exception in sendMessageAsync", ex);
            return null;
        });
    }

    @Transactional
    @Override
    public void importCampaignFiles(MultipartFile importFile, Long campaignId, Long userId) {
        Campaigns campaign = campaignsRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<ImportFileDto> importFileDtoList = parseExcelFile(importFile);

        List<SmsMessages> messages = importFileDtoList.stream()
                .map(dto -> createSmsMessage(dto, campaign, user))
                .collect(Collectors.toList());
        // Save all messages to the database before sending
        List<SmsMessages> savedMessages = smsMessagesRepository.saveAll(messages);
        // Asynchronously send the messages
        savedMessages.forEach(this::sendMessageAsync);
    }

    @Transactional
    @Override
    public void saveUnicastMessage(SmsMessagesDto smsMessagesDto) {
        Campaigns campaign = campaignsRepository.findById(smsMessagesDto.getCampaign_id())
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + smsMessagesDto.getCampaign_id()));
        User user = userRepository.findById(smsMessagesDto.getId_user())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + smsMessagesDto.getId_user()));

        if (smsMessagesDto.getRecipientNumber().isEmpty() || !isValidNineDigitNumber(smsMessagesDto.getRecipientNumber())) {
            throw new ApiException("Le destinataire avec ce contact " + smsMessagesDto.getRecipientNumber() + " est incorrect");
        }
//        SmsMessages smsMessages= this.createUnicastMessage(campaign,user,smsMessagesDto);
//        sendSmsMessage(smsMessages);
        // Create and save the unicast message
        SmsMessages smsMessage = createUnicastMessage(campaign, user, smsMessagesDto);
        SmsMessages savedMessage = smsMessagesRepository.save(smsMessage);
        // Asynchronously send the saved message
        sendMessageAsync(savedMessage);
    }

    private boolean isValidNineDigitNumber(String value) {
        // Check if the value is not null and matches exactly 9 digits
        return value != null && value.matches("\\d{9}");
    }

    @Override
    public List<SmsMessagesDto> getAllMessagesByCampaign(Long campaignId) {
        List<SmsMessages> smsMessages = smsMessagesRepository.findAllByCampaignsId(campaignId);
        return smsMessages.stream().map(messages -> mapper.map(messages,SmsMessagesDto.class)).collect(Collectors.toList());
    }

    @Override
    public SmsMessageResponse getAllMessagesByCampaign(Long campaignId, int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        //Create instance of pagination
        Pageable pageable=PageRequest.of(pageNo, pageSize, sort);
        Page<SmsMessages> smsMessages = smsMessagesRepository.findAllByCampaignsId(campaignId,pageable);
        // get Content from page object
        List<SmsMessages> listOfMesssage= smsMessages.getContent();
        List<SmsMessagesDto> content = listOfMesssage.stream().map(messages -> mapper.map(messages,SmsMessagesDto.class))
                                                             .collect(Collectors.toList());
        SmsMessageResponse smsMessageResponse = new SmsMessageResponse();
        smsMessageResponse.setContent(content);
        smsMessageResponse.setPageNo(smsMessages.getNumber());
        smsMessageResponse.setPageSize(smsMessages.getSize());
        smsMessageResponse.setTotalElements(smsMessages.getTotalElements());
        smsMessageResponse.setTotalPages(smsMessages.getTotalPages());
        smsMessageResponse.setLast(smsMessages.isLast());
        return smsMessageResponse;
    }

    @Override
    public SmsMessageResponse searchCampaignMessages(String status, Long campaignId, int pageNo, int pageSize, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        //Create instance of pagination
        Pageable pageable=PageRequest.of(pageNo, pageSize, sort);

        Page<SmsMessages> smsMessages = smsMessagesRepository.findAllByCampaignsIdAndStatusContaining(campaignId,status,pageable);
        // get Content from page object
        List<SmsMessages> listOfMesssage= smsMessages.getContent();
        List<SmsMessagesDto> content = listOfMesssage.stream().map(messages -> mapper.map(messages,SmsMessagesDto.class))
                .collect(Collectors.toList());

        SmsMessageResponse smsMessageResponse = new SmsMessageResponse();
        smsMessageResponse.setContent(content);
        smsMessageResponse.setPageNo(smsMessages.getNumber());
        smsMessageResponse.setPageSize(smsMessages.getSize());
        smsMessageResponse.setTotalElements(smsMessages.getTotalElements());
        smsMessageResponse.setTotalPages(smsMessages.getTotalPages());
        smsMessageResponse.setLast(smsMessages.isLast());

        return smsMessageResponse;
    }

    @Override
    // Modify searchMessages to accept MessageStatus
    public Page<SmsMessagesDto> searchMessages(String status, int page, int size,Long campaignId) {
        Page<SmsMessages> smsMessagesPage = smsMessagesRepository.findAllByCampaignsIdAndStatusContaining(campaignId, status, PageRequest.of(page, size));
        return smsMessagesPage.map(this::convertToDto);
    }

    @Override
    public Iterable<SmsMessages> getSmsMessages(Long campaignId) {
        return smsMessagesRepository.findAllByCampaignsId(campaignId);
    }

    @Override
    public Stats getStats() {
        Stats stats = new Stats();

        try {
            // Attempt to get OAuth token
            TokenResponse tokenResponse = orangeSmsService.getOAuthToken();

            if (tokenResponse != null && tokenResponse.getStatus() == 200) {
                BalanceResponse balanceResponse = orangeSmsService.getSmsBalance(tokenResponse.getToken());

                // Set balance and other fields if balance response is valid
                stats.setBalance(balanceResponse != null ? balanceResponse.getAvailableUnits() : 0L);
                stats.setStatus(balanceResponse != null ? balanceResponse.getStatus() : "UNKNOWN");

                // Set expiration date, using a default if not provided
                stats.setExpiredDate(balanceResponse != null && balanceResponse.getExpirationDate() != null
                        ? balanceResponse.getExpirationDate()
                        : LocalDateTime.MAX);  // Default to a distant future date if expiration is unavailable
            } else {
                System.err.println("Failed to retrieve OAuth token or token status not OK. Status: "
                        + (tokenResponse != null ? tokenResponse.getStatus() : "null"));
                stats.setBalance(0L);  // Default to zero if token retrieval fails
                stats.setStatus("TOKEN_ERROR");
                stats.setExpiredDate(LocalDateTime.MAX);
            }

            // Retrieve counts for sent and failed messages
            stats.setNbreSentMessage(smsMessagesRepository.countAllByStatus(MessageStatus.SENT.toString()));
            stats.setNbreFailledMessage(smsMessagesRepository.countAllByStatus(MessageStatus.FAILED.toString()));

        } catch (Exception e) {
            System.err.println("Error occurred while retrieving statistics: " + e.getMessage());
            // Set default values in case of an exception
            stats.setBalance(0L);
            stats.setStatus("ERROR");
            stats.setExpiredDate(LocalDateTime.MAX);
            stats.setNbreSentMessage(0);
            stats.setNbreFailledMessage(0);
        }

        return stats;
    }





    @Override
    public List<SmsMessagesDto> getAllMessages() {
        List<SmsMessages> listMessages = smsMessagesRepository.findAll();
        return listMessages.stream().map(smsMessages -> mapper.map(smsMessages,SmsMessagesDto.class)).collect(Collectors.toList());
    }

    @Override
    public Page<SmsMessagesDto> getMessages(int page, int size,Long campaignId) {
        Page<SmsMessages> smsMessagesPage = smsMessagesRepository.findAllByCampaignsId(campaignId, PageRequest.of(page, size));
        return smsMessagesPage.map(this::convertToDto);
    }



    // Helper method to convert SmsMessages entity to SmsMessagesDto
    private SmsMessagesDto convertToDto(SmsMessages smsMessages) {
        SmsMessagesDto dto = new SmsMessagesDto();
        dto.setId(smsMessages.getId());
        dto.setRecipientNumber(smsMessages.getRecipientNumber());
        dto.setSentAt(smsMessages.getSentAt());
        dto.setStatus(smsMessages.getStatus());
        // Map other fields as necessary
        return dto;
    }


    private List<ImportFileDto> parseExcelFile(MultipartFile importFile) {
        try (Workbook workbook = FileFactory.getWorkbookStream(importFile)) {
            return ExcelUtils.getImportData(workbook, ImportConfig.customerImport);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file", e);
        }
    }

    private SmsMessages createUnicastMessage(Campaigns campaign, User user,SmsMessagesDto smsMessagesDto) {
        SmsMessages message = new SmsMessages();
        message.setCampaigns(campaign);
        message.setUser(user);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.PENDING.toString());

        message.setRecipientNumber(smsMessagesDto.getRecipientNumber());
        message.setMessage(smsMessagesDto.getMessage());
        return message;
    }

    private SmsMessages createSmsMessage(ImportFileDto dto, Campaigns campaign, User user) {
        SmsMessages message = new SmsMessages();
        message.setCampaigns(campaign);
        message.setUser(user);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.PENDING.toString());
        message.setRecipientNumber(dto.getContact());
        message.setMessage(campaign.getName());
        return message;
    }

    private void sendSmsMessages(List<SmsMessages> messages) {
        messages.parallelStream().forEach(this::sendAndSaveSmsMessage);
    }

    private void sendSmsMessage(SmsMessages messages){
         this.sendAndSaveSmsMessage(messages);
    }

    private void sendAndSaveSmsMessage(SmsMessages message) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean sent = isNumeric(message.getRecipientNumber());
                if (sent) {
                    message.setStatus(MessageStatus.SENT.toString());
                    emailService.sendSMS(message.getRecipientNumber(), message.getMessage());
                } else {
                    message.setStatus(MessageStatus.FAILED.toString());
                }
            } catch (Exception e) {
                message.setStatus(MessageStatus.FAILED.toString());
                log.error("Error sending SMS: ", e);
            } finally {
                smsMessagesRepository.save(message);
            }
        }).exceptionally(ex -> {
            log.error("Exception in sendAndSaveSmsMessage", ex);
            return null;
        });
    }

//    private void sendAndSaveSmsMessage(SmsMessages message) {
//        try {
////            TokenResponse tokenResponse = orangeSmsService.getOAuthToken();
////            int numberOfBalance= Math.toIntExact(campaigns.getTotalSms());
//            boolean sent = isNumeric(message.getRecipientNumber());
//            if(sent){
//                message.setStatus(MessageStatus.SENT.toString());
//                emailService.sendSMS(message.getRecipientNumber(), message.getMessage());
//            }else{
//                message.setStatus(MessageStatus.FAILED.toString());
//            }
//        } catch (Exception e) {
//            message.setStatus(MessageStatus.FAILED.toString());
//            log.error("Error sending SMS: ", e);
//        } finally {
//            smsMessagesRepository.save(message);
//        }
//    }

    private static boolean isNumeric(String input) {
        // Regular expression to match only digits (0-9)
        return input != null && input.matches("\\d{9}");
    }

}

