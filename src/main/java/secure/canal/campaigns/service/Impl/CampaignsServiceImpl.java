package secure.canal.campaigns.service.Impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.entity.Campaigns;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.payload.CampaignsDto;
import secure.canal.campaigns.repository.CampaignsRepository;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.service.CampaignsService;
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
    @Override
    public CampaignsDto create(CampaignsDto campaignsDto) {
        Campaigns campaigns = mapper.map(campaignsDto,Campaigns.class);
        campaigns.setCreatedAt(LocalDateTime.now());
        campaigns.setStatus(CampaignStatus.ACTIVE);
        User users = userRepository.getReferenceById(campaignsDto.getId_user());
        campaigns.setUsers(users);
        Campaigns newCampaign = campaignsRepository.save(campaigns);
        return  mapper.map(newCampaign,CampaignsDto.class);
    }

    @Override
    public List<CampaignsDto> getAllCampaigns() {
        return campaignsRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(campaigns -> mapper.map(campaigns, CampaignsDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public CampaignsDto getCampaign(Long id) {
        return campaignsRepository.findById(id)
                .map(campaign -> mapper.map(campaign, CampaignsDto.class))
                .orElse(null);
    }

}
