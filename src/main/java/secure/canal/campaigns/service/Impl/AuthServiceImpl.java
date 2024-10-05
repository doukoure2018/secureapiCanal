package secure.canal.campaigns.service.Impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.*;
import secure.canal.campaigns.enumeration.VerificationType;
import secure.canal.campaigns.exception.ApiException;
import secure.canal.campaigns.exception.BlogAPIException;
import secure.canal.campaigns.exception.ResourceNotFoundException;
import secure.canal.campaigns.form.UpdateForm;
import secure.canal.campaigns.payload.CompteDto;
import secure.canal.campaigns.payload.RoleDto;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.payload.UserPrincipal;
import secure.canal.campaigns.repository.*;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.EmailService;
import secure.canal.campaigns.utils.DateUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;
import static secure.canal.campaigns.enumeration.VerificationType.ACCOUNT;
import static secure.canal.campaigns.enumeration.VerificationType.PASSWORD;
import static secure.canal.campaigns.mapper.UserDTOMapper.fromUser;
import static secure.canal.campaigns.mapper.UserDTOMapper.fromUsers;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService, UserDetailsService {

    private final static  String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private final UserRepository userRepository;
    private final TwoFactorVerificationRepository twoFactorVerificationRepository;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AccountVerificationRepository accountVerificationRepository;
    private final EmailService emailService;
    private final ResetPasswordVerificationRepository resetPasswordVerificationRepository;
    private final UserRoleRepository userRoleRepository;

    @Value("${app.frontend.baseurl}")
    private String frontendTestBaseUrl;

    @Override
    public UserDto createUser(UserDto userDto) {

        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,"Email already exist");
        }
        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());

        user.setAddress(userDto.getAddress());
        user.setPhone(userDto.getPhone());
        user.setTitle(userDto.getTitle());
        user.setBio(userDto.getBio());
        user.setEnabled(false);
        user.setNonLocked(true);
        user.setUsingMfa(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setImageUrl("");
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        // add Roles
        Set<UserRole> userRolesSet = new HashSet<>();
        Optional<Role> userRoleOpt = roleRepository.findByName("ROLE_USER");
        if (userRoleOpt.isPresent()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(userRoleOpt.get());
            userRolesSet.add(userRole);
        } else {
            throw new BlogAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "User role not found");
        }
        user.setUserRole(userRolesSet);
        // save Information
        userRepository.save(user);
        //Insert verication account
        String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
        AccountVerification accountVerifications = new AccountVerification();
        accountVerifications.setUser(user);
        accountVerifications.setUrl(verificationUrl);
        accountVerificationRepository.save(accountVerifications);
        // Send Email
        sendEmail(userDto.getFirstName(), userDto.getEmail(), verificationUrl,ACCOUNT);
        System.out.println(verificationUrl);
        // return the user information
        // Map user to UserResponse
        return  mapToUserDTO(user);

    }

    @Override
    public UserDto createCompte(CompteDto compteDto) {

        if(userRepository.existsByEmail(compteDto.getEmail())){
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,"Email already exist");
        }
        User user = new User();
        user.setFirstName(compteDto.getFirstName());
        user.setLastName(compteDto.getLastName());
        user.setEmail(compteDto.getEmail());

        user.setAddress(compteDto.getAddress());
        user.setPhone(compteDto.getPhone());
        user.setTitle(compteDto.getTitle());
        user.setBio(compteDto.getBio());
        user.setEnabled(false);
        user.setNonLocked(true);
        user.setUsingMfa(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setImageUrl("");
        user.setPassword(passwordEncoder.encode(compteDto.getPassword()));
        // add Roles
        Set<UserRole> userRolesSet = new HashSet<>();
        Optional<Role> userRoleOpt = roleRepository.findByName(compteDto.getRoleName());
        if (userRoleOpt.isPresent()) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(userRoleOpt.get());
            userRolesSet.add(userRole);
        } else {
            throw new BlogAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "User role not found");
        }
        user.setUserRole(userRolesSet);
        // save Information
        userRepository.save(user);
        //Insert verication account
        String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
        AccountVerification accountVerifications = new AccountVerification();
        accountVerifications.setUser(user);
        accountVerifications.setUrl(verificationUrl);
        accountVerificationRepository.save(accountVerifications);
        // Send Email
        sendEmail(compteDto.getFirstName(), compteDto.getEmail(), verificationUrl,ACCOUNT);
        System.out.println(verificationUrl);
        // return the user information
        return  mapToUserDTO(user);

    }

    @Override
    public List<UserDto> getAllComptes() {
        List<User> users = userRepository.findAll();
        return mapToUsersDTO(users);
    }

    @Override
    public UserDto getDetailCompte(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                ()->new ResourceNotFoundException("Compte","id",id));
        return mapToUserDTO(user);
    }

    private void sendEmail(String firstName, String email, String verificationUrl, VerificationType verificationType) {
        CompletableFuture.runAsync(() -> emailService.sendVerificationEmail(firstName, email, verificationUrl, verificationType));

    }

    @Override
    public UserDto getUserByEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Users", "Email", email));
            return  mapToUserDTO(user);
        } catch (ResourceNotFoundException exception) {
            // Re-throwing the ResourceNotFoundException with appropriate message
            log.error("User not found: {}", email);
            throw exception; // Make sure this exception is handled by the controller advice
        } catch (Exception exception) {
            // Catch any other generic exceptions
            log.error("An error occurred while fetching the user by email: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void sendVerificationCode(UserDto userDto) {
        String expirationDate = format(addDays(new Date(), 1), DATE_FORMAT);
        String verificationCode = randomAlphabetic(8).toUpperCase();
        try{
            //DELETE_VERIFICATION_CODE_BY_USER_ID
            twoFactorVerificationRepository.deleteById(userDto.getId());
            //INSERT_VERIFICATION_CODE_QUERY
            TwoFactorVerification twoFactorVerifications =new TwoFactorVerification();
            User user = mapper.map(userDto,User.class);
            twoFactorVerifications.setUser(user);
            twoFactorVerifications.setCode(verificationCode);
            twoFactorVerifications.setExpirationDate(DateUtil.parseStringToLocalDateTime(expirationDate));
            twoFactorVerificationRepository.save(twoFactorVerifications);
            emailService.sendSMS(user.getPhone(), "From: SecureCapita \nVerification code\n" + verificationCode);
            log.info("Verification Code: {}", verificationCode);
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw  new BlogAPIException(HttpStatus.BAD_REQUEST,"An error occurred. Please try again.");
        }

    }

    // Send text message



    @Override
    public UserDto verifyCode(String email, String code) {
        if(isVerificationCodeExpired(code)) throw new ApiException("This code has expired. Please login again.");
        try {
            User users = userRepository.findByTwoFactorVerification_Code(code);

            UserDto userByEmail = getUserByEmail(email);
            if (userByEmail.getEmail().equalsIgnoreCase(userByEmail.getEmail())) {
                // Fetch the verification entity using code
                TwoFactorVerification verification = twoFactorVerificationRepository.findByCode(code)
                        .orElseThrow(() -> new ApiException("Verification code not found"));
                // Explicitly dissociate the verification from the user (optional step)
                User user = verification.getUser();
                user.setTwoFactorVerification(null);
                userRepository.save(user);  // Save the user to update the relationship
                // Now delete the verification entity
                twoFactorVerificationRepository.deleteById(verification.getId());
                return mapToUserDTO(users);
            } else {
                throw new ApiException("Invalid code. Please try again.");
            }
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("Could not find record");
        } catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    private Boolean isVerificationCodeExpired(String code) {
        if (code == null || code.isEmpty()) {
            throw new ApiException("Verification code must not be null or empty");
        }
        try {
            // Fetch the expiration status of the verification code
            Boolean isExpired = twoFactorVerificationRepository.isCodeExpired(code);
            // Handle the case where the code is not found (null check)
            if (isExpired == null) {
                throw new ApiException("Verification code not found or invalid. Please try again.");
            }
            // Return the expiration status
            return isExpired;
        } catch (ApiException ex) {
            // Custom ApiException, pass it along
            throw ex;
        } catch (Exception exception) {
            // Catch any other unexpected exception and throw a general error
            throw new ApiException("An error occurred. Please try again.");
        }
    }


    @Override
    public void resetPassword(String email) {
        // Normalize and validate the email address
        String normalizedEmail = email.trim().toLowerCase();
        // Check if an account exists for the provided email
        if (getEmailCount(normalizedEmail) <= 0) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "There is no account for this email address.");
        }
        try {
            // Set expiration date to 24 hours from now
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(1);
            // Retrieve the user by email
            UserDto userDto = getUserByEmail(normalizedEmail);
            // Generate a new verification URL
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), PASSWORD.getType());
            // Delete any existing password verification entries for this user
            resetPasswordVerificationRepository.deleteByUserId(userDto.getId());
            // Create a new ResetPasswordVerifications entry
            ResetPasswordVerification resetPasswordVerification = new ResetPasswordVerification();
            resetPasswordVerification.setUser(mapper.map(userDto, User.class));
            resetPasswordVerification.setUrl(verificationUrl);
            resetPasswordVerification.setExpirationDate(expirationDate);
            // Save the new reset password verification entry
            resetPasswordVerificationRepository.save(resetPasswordVerification);
            // Send the verification email
            sendEmail(userDto.getFirstName(), normalizedEmail, verificationUrl, PASSWORD);
            // Log the verification URL for debugging purposes
            log.info("Verification URL sent: {}", verificationUrl);
        } catch (BlogAPIException ex) {
            // Log the specific error message and rethrow the exception
            log.error("Password reset error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            // Log unexpected errors and throw a generic error message
            log.error("Unexpected error during password reset: {}", ex.getMessage(), ex);
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "An error occurred. Please try again.");
        }
    }

    @Override
    public UserDto verifyPasswordKey(String key) {
        // Check if the link has expired
        if (isLinkExpired(key, PASSWORD)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "This link has expired. Please reset your password again.");
        }
        try {
            // Get the verification URL
            String verificationUrl = getVerificationUrl(key, PASSWORD.getType());
            // Find the user by the verification URL and map to UserDto
            User user = userRepository.findByResetPasswordVerificationUrl(verificationUrl);
            return mapToUserDTO(user);
        } catch (BlogAPIException ex) {
            // Re-throw BlogAPIException with the original context
            log.error("Password verification failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            // Log unexpected errors and wrap them in a BlogAPIException
            log.error("Unexpected error during password verification: {}", ex.getMessage(), ex);
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "This link is not valid. Please reset your password again.");
        }
    }
    private Boolean isLinkExpired(String key, VerificationType verificationType) {
        try {
            String verificationUrl = getVerificationUrl(key, verificationType.getType());
            return userRepository.isExpired(verificationUrl);
        } catch (BlogAPIException ex) {
            // Log the specific error message from the BlogAPIException
            log.error("Verification failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            // Log any other unexpected exceptions
            log.error("Unexpected error during verification: {}", ex.getMessage(), ex);
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "This link is not valid. Please reset your password again.");
        }
    }


    @Override
    public void updatePasswordWithKey(String key, String password, String confirmPassword) {
        if(!password.equals(confirmPassword)) throw new BlogAPIException(HttpStatus.BAD_REQUEST,"Passwords don't match. Please try again.");
        try {
            String verificationUrl = getVerificationUrl(key,PASSWORD.getType());
            User user = userRepository.findByResetPasswordVerificationUrl(verificationUrl);
            //UPDATE_USER_PASSWORD_BY_URL_QUERY
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            //DELETE_VERIFICATION_BY_URL_QUERY
            resetPasswordVerificationRepository.deleteByUserId(user.getId());
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,"An error occurred. Please try again.oo");
        }
    }

    @Override
    public void updatePasswordWithIdUser(Long userId, String password, String confirmPassword) {
        if(!password.equals(confirmPassword)) throw new BlogAPIException(HttpStatus.BAD_REQUEST,"Passwords don't match. Please try again.");
        try {
            User userInfo=userRepository.getReferenceById(userId);
            //UPDATE_USER_PASSWORD_BY_URL_QUERY
            userInfo.setPassword(passwordEncoder.encode(password));
            userRepository.save(userInfo);
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new BlogAPIException(HttpStatus.BAD_REQUEST,"An error occurred. Please try again.oo");
        }
    }

    @Override
    public UserDto verifyAccountKey(String key) {
        try {
            // Retrieve the verification URL using the provided key and account type
            String verificationUrl = getVerificationUrl(key, ACCOUNT.getType());
            // Find the user associated with the account verification URL
            User user = userRepository.findByAccountVerificationUrl(verificationUrl);
            // Enable the user's account
            user.setEnabled(true);
            // Save the updated user entity
            User updatedUser = userRepository.save(user);
            // Map the updated user entity to a UserDto
            return mapToUserDTO(updatedUser);
        } catch (Exception ex) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "The link is not valid...");
        }
    }


    @Override
    public UserDto updateUserDetails(UpdateForm updateUser) {

        // Fetch the existing user from the database
        User existingUser = userRepository.findById(updateUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + updateUser.getId()));
        // Update the fields from the UpdateForm
        existingUser.setFirstName(updateUser.getFirstName());
        existingUser.setLastName(updateUser.getLastName());
        existingUser.setEmail(updateUser.getEmail());
        existingUser.setPhone(updateUser.getPhone());
        existingUser.setAddress(updateUser.getAddress());
        existingUser.setTitle(updateUser.getTitle());
        existingUser.setBio(updateUser.getBio());
        // Save the updated user
        User updatedUser = userRepository.save(existingUser);
        // Map the updated user entity to UserDto and return
        return mapToUserDTO(updatedUser);
    }


    @Override
    public UserDto getUserById(Long userId) {
        try {
            User users = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Users", "userId", userId));
            return mapToUserDTO(users);
        } catch (ResourceNotFoundException exception) {
            // Re-throwing the ResourceNotFoundException with appropriate message
            log.error("User not found: {}", userId);
            throw exception; // Make sure this exception is handled by the controller advice
        } catch (Exception exception) {
            // Catch any other generic exceptions
            log.error("An error occurred while fetching the user by email: {}", exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updatePassword(Long userId, String currentPassword, String newPassword, String confirmNewPassword) {
        // Check if the new password matches the confirmation password
        if (!newPassword.equals(confirmNewPassword)) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Passwords don't match. Please try again.");
        }
        User user = userRepository.getReferenceById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Incorrect current password. Please try again.");
        }
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } catch (Exception e) {
            // Handle any unexpected exceptions that might occur during the password update
            throw new BlogAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while updating the password. Please try again later.");
        }
    }


    @Override
    public void updateUserRole(Long userId, String roleName) {
        log.info("Updating role for user id: {}", userId);
        try {
            UserRole userRoles = userRoleRepository.findUserRolesByUserId(userId);
            if (userRoles == null) {
                throw new ApiException("No user role found for userId: " + userId);
            }
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ApiException("No role found for roleName: " + roleName));

            userRoles.setRole(role);
            userRoleRepository.save(userRoles);

            log.info("Role updated successfully for user id: {}", userId);

        } catch (EmptyResultDataAccessException exception) {
            log.error("Role not found by name: {} for user id: {}", roleName, userId);
            throw new ApiException("No role found for roleName: " + roleName);
        } catch (ApiException apiException) {
            log.error("ApiException: {}", apiException.getMessage());
            throw apiException;
        } catch (Exception exception) {
            log.error("An unexpected error occurred while updating role for user id: {}. Error: {}", userId, exception.getMessage(), exception);
            throw new ApiException("An unexpected error occurred. Please try again.");
        }
    }

    @Override
    public void updateUserRoleByAdmin(Long idCompte, String roleName) {
        log.info("Updating role for user id: {}", idCompte);
        try {
            UserRole userRoles = userRoleRepository.findUserRolesByUserId(idCompte);
            if (userRoles == null) {
                throw new ApiException("No user role found for userId: " + idCompte);
            }
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ApiException("No role found for roleName: " + roleName));

            userRoles.setRole(role);
            userRoleRepository.save(userRoles);

            log.info("Role updated successfully for user id: {}", idCompte);

        } catch (EmptyResultDataAccessException exception) {
            log.error("Role not found by name: {} for user id: {}", roleName, idCompte);
            throw new ApiException("No role found for roleName: " + roleName);
        } catch (ApiException apiException) {
            log.error("ApiException: {}", apiException.getMessage());
            throw apiException;
        } catch (Exception exception) {
            log.error("An unexpected error occurred while updating role for user id: {}. Error: {}", idCompte, exception.getMessage(), exception);
            throw new ApiException("An unexpected error occurred. Please try again.");
        }
    }

    @Override
    public void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked) {
        // Get the instance User
        User user = userRepository.getReferenceById(userId);
        user.setEnabled(enabled);
        user.setNonLocked(notLocked);
        userRepository.save(user);
    }

    @Override
    public void updateAccountSettingsByAdmin(Long userId, Boolean enabled, Boolean notLocked)
    {
        User user = userRepository.getReferenceById(userId);
        user.setEnabled(enabled);
        user.setNonLocked(notLocked);
        userRepository.save(user);
    }

    @Override
    public UserDto toggleMfa(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                ()-> new ResourceNotFoundException("Users","Email","email"));
        if(isBlank(user.getPhone())) { throw new ApiException("You need a phone number to change Multi-Factor Authentication"); }
        user.setUsingMfa(!user.getUsingMfa());
        try {
            userRepository.save(user);
            return mapToUserDTO(user);
        }catch (Exception exception){
            log.error(exception.getMessage());
            throw new ApiException("Unable to update Multi-Factor Authentication");
        }
    }

    @Override
    public void updateImage(UserDto user, MultipartFile image) {
        String userImageUrl = setUserImageUrl(user.getEmail());
        user.setImageUrl(userImageUrl);
        saveImage(user.getEmail(), image);
        User updateUser = userRepository.getReferenceById(user.getId());
        updateUser.setImageUrl(userImageUrl);
        //Users updateImageUser = mapper.map(user,Users.class);
        userRepository.save(updateUser);
    }

    private void saveImage(String email, MultipartFile image) {
        Path fileStorageLocation = Paths.get(System.getProperty("user.home") + "/IdeaProjects/securecapita/src/main/resources/imagesProfiles/").toAbsolutePath().normalize();
        if(!Files.exists(fileStorageLocation)) {
            try {
                Files.createDirectories(fileStorageLocation);
            } catch (Exception exception) {
                log.error(exception.getMessage());
                throw new BlogAPIException(HttpStatus.BAD_REQUEST,"Unable to create directories to save image");
            }
            log.info("Created directories: {}", fileStorageLocation);
        }
        try {
            Files.copy(image.getInputStream(), fileStorageLocation.resolve(email + ".png"), REPLACE_EXISTING);
        } catch (IOException exception) {
            log.error(exception.getMessage());
            throw new ApiException(exception.getMessage());
        }
        log.info("File saved in: {} folder", fileStorageLocation);
    }

    private String setUserImageUrl(String email) {
        return fromCurrentContextPath().path("/auth/secureapi/image/" + email + ".png").toUriString();
    }

    private String getVerificationUrl(String key, String type) {
        //return fromCurrentContextPath().path("/auth/secureapi/verify/" + type + "/" + key).toUriString();
        return frontendTestBaseUrl + "/auth/secureapi/verify/" + type + "/" + key;
    }

    private Integer getEmailCount(String email){
        return userRepository.countUsersByEmail(email);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserDto userDto = getUserByEmail(email);

        if(userDto == null) {
            log.error("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        } else {
            log.info("User found in the database: {}", email);
            return new UserPrincipal(mapper.map(userDto,User.class), mapper.map(roleRepository.getRoleByUserId(userDto.getId()),Role.class));
        }
    }

    private UserDto mapToUserDTO(User user) {
        return fromUser(user, mapper.map(roleRepository.getRoleByUserId(user.getId()),Role.class));
    }

    private List<UserDto> mapToUsersDTO(List<User> users) {
        // Create a map of user IDs to RoleDto objects
        Map<Long, RoleDto> rolesMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> mapper.map(roleRepository.getRoleByUserId(user.getId()), RoleDto.class)));

        // Now map users to UserDto with RoleDto
        return fromUsers(users, rolesMap);
    }





}


