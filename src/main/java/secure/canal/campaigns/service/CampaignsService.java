package secure.canal.campaigns.service;

import secure.canal.campaigns.payload.CampaignsDto;

import java.util.List;

public interface CampaignsService {

    CampaignsDto create(CampaignsDto campaignsDto);

    List<CampaignsDto> getAllCampaigns();

    CampaignsDto getCampaign(Long id);


}