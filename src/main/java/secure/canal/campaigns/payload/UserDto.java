package secure.canal.campaigns.payload;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDto {
    private Long id;
    private String firstName;
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
    private String permissions;

}
