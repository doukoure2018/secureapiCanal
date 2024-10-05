package secure.canal.campaigns.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CompteDto {

    private Long id;
    private String firstName;
    @NotEmpty
    private String lastName;
    @Email(message = "Invalid email. Please enter a valid email address")
    private String email;
    private String password;
    private String address;
    private String phone;
    private String title;
    private String bio;
    private Boolean enabled;
    private Boolean nonLocked;
    private Boolean usingMfa;
    private LocalDateTime createdAt;
    private String imageUrl;
    private String roleName;
}
