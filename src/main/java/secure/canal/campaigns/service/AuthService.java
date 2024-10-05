package secure.canal.campaigns.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.form.UpdateForm;
import secure.canal.campaigns.payload.CompteDto;
import secure.canal.campaigns.payload.UserDto;

import java.util.List;

public interface AuthService {

    UserDto createUser(UserDto user);
    UserDto createCompte(CompteDto compteDto);

    UserDto getDetailCompte(Long id);

    List<UserDto> getAllComptes();

    UserDto getUserByEmail(String email);
    void sendVerificationCode(UserDto user);
    UserDto verifyCode(String email, String code);
    void resetPassword(String email);
    UserDto verifyPasswordKey(String key);
    void updatePasswordWithKey(String key, String password, String confirmPassword);
    void updatePasswordWithIdUser(Long userId, String password, String confirmPassword);
    UserDto verifyAccountKey(String key);
    UserDto updateUserDetails(UpdateForm user);
    UserDto getUserById(Long userId);
    void updatePassword(Long userId, String currentPassword, String newPassword, String confirmNewPassword);
    void updateUserRole(Long userId, String roleName);

    void updateUserRoleByAdmin(Long idCompte, String roleName);
    void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked);
    void updateAccountSettingsByAdmin(Long userId, Boolean enabled, Boolean notLocked);
    UserDto toggleMfa(String email);
    void updateImage(UserDto user, MultipartFile image);



}