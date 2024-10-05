package secure.canal.campaigns.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.payload.SmsMessagesDto;
import secure.canal.campaigns.stats.Stats;

import java.util.List;

public interface SmsMessagesService {

    SmsMessagesDto create(SmsMessagesDto smsMessagesDto);

    void importCampaignFiles(MultipartFile importFile, Long campaign_id, Long id_user);

    Stats getStats();

    List<SmsMessagesDto> getAllMessages();

    Page<SmsMessagesDto> getMessages(int page, int size);

    Page<SmsMessages> searchMessages(String name, int page, int size);


}
