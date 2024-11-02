package secure.canal.campaigns.payload;

import jakarta.validation.constraints.NotEmpty;
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
    @NotEmpty(message = "Contact should not be empty")
    private String recipientNumber;
    @NotEmpty(message = "Message should not be empty")
    private String message;
    private String status;
    private LocalDateTime sentAt;
    private Long campaign_id;
    private Long id_user;
}
