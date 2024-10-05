package secure.canal.campaigns.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Stats {

    private int balance;
    private int nbreSentMessage;
    private int nbreFailledMessage;

}
