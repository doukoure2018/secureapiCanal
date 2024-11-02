package secure.canal.campaigns.service.Impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.entity.Campaigns;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.enumeration.MessageStatus;
import secure.canal.campaigns.exception.ApiException;
import secure.canal.campaigns.exception.ResourceNotFoundException;
import secure.canal.campaigns.form.UpdateCampaingForm;
import secure.canal.campaigns.form.UpdateMessageForm;
import secure.canal.campaigns.payload.BalanceResponse;
import secure.canal.campaigns.payload.CampaignsDto;
import secure.canal.campaigns.payload.TokenResponse;
import secure.canal.campaigns.repository.CampaignsRepository;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.service.CampaignsService;
import secure.canal.campaigns.service.OrangeSmsService;
import secure.canal.campaigns.utils.CampaignStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignsServiceImpl implements CampaignsService {

    private final CampaignsRepository campaignsRepository;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    private final OrangeSmsService orangeSmsService;
    @Override
    public CampaignsDto create(CampaignsDto campaignsDto) {
        if(campaignsDto.getMode().equalsIgnoreCase("BROADCAST") && campaignsDto.getTotalSms()==0) throw new ApiException("Mode BROADCAST: TotalSms > 0");
        Campaigns campaigns = mapper.map(campaignsDto,Campaigns.class);
        campaigns.setCreatedAt(LocalDateTime.now());
        campaigns.setStatus(CampaignStatus.ACTIVE.toString());
        User users = userRepository.getReferenceById(campaignsDto.getId_user());
        campaigns.setUsers(users);
        Campaigns newCampaign = campaignsRepository.save(campaigns);
        return  mapper.map(newCampaign,CampaignsDto.class);
    }

    @Override
    public List<CampaignsDto> getAllCampaigns() {
        return campaignsRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(campaigns -> mapper.map(campaigns, CampaignsDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public CampaignsDto getCampaign(Long id) {
        return campaignsRepository.findById(id)
                .map(campaign -> mapper.map(campaign, CampaignsDto.class))
                .orElse(null);
    }

    @Override
    public CampaignsDto updateCampaignById(Long idCampaign, UpdateCampaingForm updateCampaingForm) {
        Campaigns campaigns = campaignsRepository.getReferenceById(idCampaign);
        campaigns.setName(updateCampaingForm.getName());
        campaigns.setMode(updateCampaingForm.getMode());
        campaigns.setTotalSms(updateCampaingForm.getTotalSms());
        campaigns.setCreatedAt(LocalDateTime.now());
        campaigns.setStatus(updateCampaingForm.getStatus());
        Campaigns updateCampagin = campaignsRepository.save(campaigns);
        return mapper.map(updateCampagin,CampaignsDto.class);
    }

    @Override
    public CampaignsDto updateCampaignMessageById(Long idCampaign, UpdateMessageForm updateMessageForm) {
        Campaigns campaigns = campaignsRepository.findById(idCampaign).orElseThrow(
                ()-> new ApiException("Campaign with this Id : "+idCampaign+" does not exist"));
        campaigns.setName(updateMessageForm.getMessage());

        Campaigns updateCampaign = campaignsRepository.save(campaigns);
        return mapper.map(updateCampaign,CampaignsDto.class);
    }

    @Override
    public BalanceResponse getBalanceSms() {
        try {
            TokenResponse tokenResponse = orangeSmsService.getOAuthToken();
            if (tokenResponse != null && tokenResponse.getStatus() == 200) {
                return orangeSmsService.getSmsBalance(tokenResponse.getToken());
            } else {
                System.err.println("Failed to retrieve OAuth token, status: " + (tokenResponse != null ? tokenResponse.getStatus() : "null"));
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error occurred while retrieving SMS balance: " + e.getMessage());
            return null;
        }
    }



}
