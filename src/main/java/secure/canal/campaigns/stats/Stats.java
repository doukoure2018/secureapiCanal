package secure.canal.campaigns.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Stats {

    private Long balance;
    private int nbreSentMessage;
    private int nbreFailledMessage;
    private String status;
    private LocalDateTime expiredDate;

}
