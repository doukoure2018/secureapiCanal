package secure.canal.campaigns.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateForm {

    @NotNull(message = "ID can not be null or Empty")
    private Long id;
    @NotEmpty(message = "firstName can not be empty or null")
    private String firstName;
    @NotEmpty(message = "lastName can not be empty or null")
    private String lastName;
    @NotEmpty(message = "Email cannot be empty")
    @Email(message = "Invalid email. Please enter a valid email address")
    private String email;
    @Pattern(regexp = "^\\d{9}$", message = "Invalid phone number")
    private String phone;
    private String address;
    private String title;
    private String bio;
}
