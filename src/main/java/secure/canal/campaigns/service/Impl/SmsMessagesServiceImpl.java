package secure.canal.campaigns.service.Impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.Campaigns;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.enumeration.MessageStatus;
import secure.canal.campaigns.payload.ImportFileDto;
import secure.canal.campaigns.payload.SmsMessagesDto;
import secure.canal.campaigns.payload.TokenResponse;
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
        SmsMessages smsMessages =mapper.map(smsMessagesDto,SmsMessages.class);
        // get Campaign
        Campaigns campaigns = campaignsRepository.getReferenceById(smsMessagesDto.getCampaign_id());
        // get User
        User users = userRepository.getReferenceById(smsMessagesDto.getId_user());
        smsMessages.setCampaigns(campaigns);
        smsMessages.setUser(users);
        smsMessages.setSentAt(LocalDateTime.now());
        smsMessages.setStatus(MessageStatus.SENT);
        SmsMessages newSmsMessage = smsMessagesRepository.save(smsMessages);
        emailService.sendSMS(smsMessagesDto.getRecipientNumber(), smsMessagesDto.getMessage());
        return mapper.map(newSmsMessage,SmsMessagesDto.class);
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

        sendSmsMessages(messages);
    }

    @Override
    public Stats getStats() {
        Stats stats = new Stats();
        TokenResponse tokenResponse = orangeSmsService.getOAuthToken();
        if(tokenResponse.getStatus() == 200){
            stats.setBalance(this.orangeSmsService.getSmsBalance(tokenResponse.getToken()));
        }else {
            stats.setBalance(this.orangeSmsService.getSmsBalance(String.valueOf(0)));
        }
        stats.setNbreSentMessage(smsMessagesRepository.countAllByStatus(MessageStatus.SENT));
        stats.setNbreFailledMessage(smsMessagesRepository.countAllByStatus(MessageStatus.FAILED));
        return  stats;
    }

    @Override
    public Page<SmsMessages> searchMessages(String name, int page, int size) {
        return smsMessagesRepository.findByStatusContaining(name, of(page, size));
    }

    @Override
    public List<SmsMessagesDto> getAllMessages() {
        List<SmsMessages> listMessages = smsMessagesRepository.findAll();
        return listMessages.stream().map(smsMessages -> mapper.map(smsMessages,SmsMessagesDto.class)).collect(Collectors.toList());
    }

    @Override
    public Page<SmsMessagesDto> getMessages(int page, int size) {
        Page<SmsMessages> smsMessagesPage = smsMessagesRepository.findAll(PageRequest.of(page, size));
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

    private SmsMessages createSmsMessage(ImportFileDto dto, Campaigns campaign, User user) {
        SmsMessages message = new SmsMessages();
        message.setCampaigns(campaign);
        message.setUser(user);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.PENDING);
        message.setRecipientNumber(dto.getContact());
        message.setMessage(dto.getMessage());
        return message;
    }

    private void sendSmsMessages(List<SmsMessages> messages) {
        messages.parallelStream().forEach(this::sendAndSaveSmsMessage);
    }

    private void sendAndSaveSmsMessage(SmsMessages message) {
        try {
            TokenResponse tokenResponse = orangeSmsService.getOAuthToken();
            int numberOfBalance= orangeSmsService.getSmsBalance(tokenResponse.getToken());
            boolean sent = isNumeric(message.getRecipientNumber());
            if(numberOfBalance > 0 && sent){
                message.setStatus(MessageStatus.SENT);
                emailService.sendSMS(message.getRecipientNumber(), message.getMessage());
            }else{
                message.setStatus(MessageStatus.FAILED);
            }
        } catch (Exception e) {
            message.setStatus(MessageStatus.FAILED);
            log.error("Error sending SMS: ", e);
        } finally {
            smsMessagesRepository.save(message);
        }
    }

    private static boolean isNumeric(String input) {
        // Regular expression to match only digits (0-9)
        return input != null && input.matches("\\d{9}");
    }

}

