package secure.canal.campaigns.service;

import secure.canal.campaigns.form.UpdateCampaingForm;
import secure.canal.campaigns.form.UpdateMessageForm;
import secure.canal.campaigns.payload.BalanceResponse;
import secure.canal.campaigns.payload.CampaignsDto;

import java.util.List;

public interface CampaignsService {

    CampaignsDto create(CampaignsDto campaignsDto);

    List<CampaignsDto> getAllCampaigns();

    CampaignsDto getCampaign(Long id);

    CampaignsDto updateCampaignById(Long idCampaign, UpdateCampaingForm updateCampaingForm);

    CampaignsDto updateCampaignMessageById(Long idCampaign, UpdateMessageForm message);

    BalanceResponse getBalanceSms();



}