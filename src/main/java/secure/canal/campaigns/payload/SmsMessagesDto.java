package secure.canal.campaigns.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import secure.canal.campaigns.enumeration.MessageStatus;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SmsMessagesDto {

    private Long id;
    private String recipientNumber;
    private String message;
    private MessageStatus status;
    private LocalDateTime sentAt;
    private Long campaign_id;
    private Long id_user;

}
