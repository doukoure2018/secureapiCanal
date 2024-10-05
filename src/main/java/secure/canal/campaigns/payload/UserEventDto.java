package secure.canal.campaigns.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserEventDto {

    private Long id;
    private String device;
    private String ipAddress;
    private String eventType;
    private String description;
    private LocalDateTime createdAt;
}
