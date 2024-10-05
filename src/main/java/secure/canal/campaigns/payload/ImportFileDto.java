package secure.canal.campaigns.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ImportFileDto {
    private Long id;
    private String nom;
    private String prenom;
    private String region;
    private String prefecture;
    private String sousPerfecture;
    private String quartierDistrict;
    private String contact;
    private String message;
}

