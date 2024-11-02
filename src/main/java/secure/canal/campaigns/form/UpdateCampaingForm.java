package secure.canal.campaigns.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateCampaingForm {

    private String name;
    private LocalDateTime createdAt;
    private String status;
    private String mode;
    private Long totalSms;
}
