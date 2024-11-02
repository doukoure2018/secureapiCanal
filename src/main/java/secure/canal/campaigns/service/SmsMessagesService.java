package secure.canal.campaigns.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.enumeration.MessageStatus;
import secure.canal.campaigns.payload.CampaignsDto;
import secure.canal.campaigns.payload.SmsMessageResponse;
import secure.canal.campaigns.payload.SmsMessagesDto;
import secure.canal.campaigns.stats.Stats;

import java.util.List;

public interface SmsMessagesService {

    SmsMessagesDto create(SmsMessagesDto smsMessagesDto);

    void importCampaignFiles(MultipartFile importFile, Long campaign_id, Long id_user);

    Stats getStats();

    List<SmsMessagesDto> getAllMessages();

    Page<SmsMessagesDto> getMessages(int page, int size, Long campaignId);

    Page<SmsMessagesDto> searchMessages(String status, int page, int size,Long campaignId);

    Iterable<SmsMessages> getSmsMessages(Long campaignId);

    void saveUnicastMessage(SmsMessagesDto smsMessagesDto);

    List<SmsMessagesDto> getAllMessagesByCampaign(Long campaignId);

    SmsMessageResponse getAllMessagesByCampaign(Long campaignId,int pageNo, int pageSize, String sortBy, String sortDir);

    SmsMessageResponse searchCampaignMessages(String status,Long campaignId,int pageNo, int pageSize, String sortBy, String sortDir);


}
