package secure.canal.campaigns.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import secure.canal.campaigns.utils.CampaignStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CampaignsDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private CampaignStatus status;
    private Long totalSms;
    private Long id_user;
}