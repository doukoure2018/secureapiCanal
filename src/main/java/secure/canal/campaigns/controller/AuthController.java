package secure.canal.campaigns.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.Role;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.event.NewUserEvent;
import secure.canal.campaigns.exception.ApiException;
import secure.canal.campaigns.form.NewPasswordForm;
import secure.canal.campaigns.form.SettingsForm;
import secure.canal.campaigns.form.UpdateForm;
import secure.canal.campaigns.form.UpdatePasswordForm;
import secure.canal.campaigns.payload.*;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.security.JwtTokenProvider;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.EventService;
import secure.canal.campaigns.service.RoleService;
import secure.canal.campaigns.service.UserEventService;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.LocalTime.now;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated;
import static org.springframework.util.MimeTypeUtils.IMAGE_PNG_VALUE;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;
import static secure.canal.campaigns.constant.Constants.TOKEN_PREFIX;
import static secure.canal.campaigns.enumeration.EventType.*;
import static secure.canal.campaigns.utils.UserUtils.getAuthenticatedUser;
import static secure.canal.campaigns.utils.UserUtils.getLoggedInUser;

@RestController
@RequestMapping("/secureapi")
@RequiredArgsConstructor
public class AuthController {

    private  final AuthService userService;
    private final UserEventService userEventsService;
    private final RoleService rolesService;
    //private final UserRoleService userRolesService;
    private final EventService eventsService;
    private final JwtTokenProvider jwtTokenProvider;

    private final ApplicationEventPublisher publisher;
    private final ModelMapper mapper;

    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(@RequestBody @Valid UserDto userDto) throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        UserDto userDto1 = userService.createUser(userDto);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDto1))
                        .message(String.format("User account created for user %s", userDto1.getFirstName()))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @GetMapping("/comptes")
    public ResponseEntity<HttpResponse> getAllComptes(@AuthenticationPrincipal UserDto user) {
        //List<UserDto> comptes = userService.getAllComptes();
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "comptes", userService.getAllComptes()))
                        .message(String.format("Comptes retreived"))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @GetMapping("/compte/{id}")
    public ResponseEntity<HttpResponse> getDetailCompte(@AuthenticationPrincipal UserDto user,@PathVariable(name = "id") Long id) {

        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserById(user.getId()),
                                "compte", userService.getDetailCompte(id),
                                "events", userEventsService.getEventsByUserId(id),
                                "roles", rolesService.getRoles()))
                        .message(String.format("Detail Compte"))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @GetMapping("/comptes/new")
    public ResponseEntity<HttpResponse> nouveauCompte(@AuthenticationPrincipal UserDto user) {

        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail()),
                                "roles", rolesService.getRoles()))
                        .message(String.format("Create de nouveau Compte"))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @PostMapping("/createAccount")
    public ResponseEntity<HttpResponse> createUser(@AuthenticationPrincipal UserDto user, @RequestBody @Valid CompteDto compteDto) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        UserDto userDto1 = userService.createCompte(compteDto);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserByEmail(user.getEmail())))
                        .message(String.format("User account created for user %s", userDto1.getFirstName()))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @PostMapping(value = {"/login", "/signin"})
    public ResponseEntity<HttpResponse> login(@RequestBody @Valid LoginDto loginDto){
        UserDto userDto = authenticate(loginDto.getEmail(),loginDto.getPassword());
        return Optional.ofNullable(userDto.getUsingMfa()).orElse(false)
                ? sendVerificationCode(userDto)
                : sendResponse(userDto);
    }

    private UserDto authenticate(String email, String password) {
        UserDto userByEmail = userService.getUserByEmail(email);
        try {
            if (userByEmail != null) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT));
            }
            Authentication authentication = authenticationManager.authenticate(unauthenticated(email, password));
            UserDto loggedInUser = getLoggedInUser(authentication);
            if (!loggedInUser.getUsingMfa()) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT_SUCCESS));
            }
            return loggedInUser;
        } catch (Exception exception) {
            if (userByEmail != null) {
                publisher.publishEvent(new NewUserEvent(email, LOGIN_ATTEMPT_FAILURE));
            }
            throw new ApiException(exception.getMessage());
        }
    }


    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> profile(Authentication authentication){
        UserDto userResponse = userService.getUserByEmail(getAuthenticatedUser(authentication).getEmail());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userResponse, "events", userEventsService.getEventsByUserId(userResponse.getId()), "roles", rolesService.getRoles()))
                        .message("Profile Retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());

    }

    @PatchMapping("/update")
    public ResponseEntity<HttpResponse> updateUser(@RequestBody @Valid UpdateForm updateForm) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        UserDto updateUserDto = userService.updateUserDetails(updateForm);
        publisher.publishEvent(new NewUserEvent(updateUserDto.getEmail(), PROFILE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", updateUserDto, "events", userEventsService.getEventsByUserId(updateUserDto.getId()), "roles", rolesService.getRoles()))
                        .message("User updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    // START - To reset password when user is not logged in
    @GetMapping("/verify/code/{email}/{code}")
    public ResponseEntity<HttpResponse> verifyCode(
            @PathVariable("email") String email,
            @PathVariable("code") String code
    ){
        UserDto userDto=userService.verifyCode(email,code);
        publisher.publishEvent(new NewUserEvent(userDto.getEmail(),LOGIN_ATTEMPT_SUCCESS));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDto, "access_token", jwtTokenProvider.createAccessToken(getUserPrincipal(userDto))
                                , "refresh_token", jwtTokenProvider.createRefreshToken(getUserPrincipal(userDto))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/resetpassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email){
        userService.resetPassword(email);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Email sent. Please check your email to reset your password.")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/verify/account/{key}")
    public ResponseEntity<HttpResponse> verifyAccount(
            @PathVariable("key") String key) throws InterruptedException
    {
        TimeUnit.SECONDS.sleep(3);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message(userService.verifyAccountKey(key).getEnabled() ? "Account already verified" : "Account verified")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/verify/password/{key}")
    public ResponseEntity<HttpResponse> verifyPasswordUrl(
            @PathVariable("key") String key) throws InterruptedException
    {
        TimeUnit.SECONDS.sleep(3);
        UserDto userDto = userService.verifyPasswordKey(key);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDto))
                        .message("Please enter a new password")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PutMapping("/new/password")
    public ResponseEntity<HttpResponse> resetPasswordWithKey(@RequestBody @Valid NewPasswordForm form) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        userService.updatePasswordWithIdUser(form.getUserId(), form.getPassword(), form.getConfirmPassword());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .message("Password reset successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }
    // END - To reset password when user is not logged in

    @PatchMapping("/update/password")
    public ResponseEntity<HttpResponse> updatePassword(Authentication authentication, @RequestBody @Valid UpdatePasswordForm form) {
        UserDto userResponse = getAuthenticatedUser(authentication);
        userService.updatePassword(userResponse.getId(), form.getCurrentPassword(), form.getNewPassword(), form.getConfirmNewPassword());
        publisher.publishEvent(new NewUserEvent(userResponse.getEmail(), PASSWORD_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userService.getUserById(userResponse.getId()), "events", userEventsService.getEventsByUserId(userResponse.getId()), "roles", rolesService.getRoles()))
                        .message("Password updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    // update user role when the user is logging
    @PatchMapping("/update/role/{roleName}")
    public ResponseEntity<HttpResponse> updateUserRole(Authentication authentication, @PathVariable("roleName") String roleName) {
        UserDto userDTO = getAuthenticatedUser(authentication);
        userService.updateUserRole(userDTO.getId(), roleName);
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), ROLE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userDTO.getId()), "events", userEventsService.getEventsByUserId(userDTO.getId()), "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Role updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/role/{roleName}/{idCompte}")
    public ResponseEntity<HttpResponse> updateUserRoleByAdmin(Authentication authentication,
                                                              @PathVariable("roleName") String roleName,
                                                              @PathVariable(name = "idCompte") Long idCompte) {
        UserDto userDTO = getAuthenticatedUser(authentication);
        userService.updateUserRoleByAdmin(idCompte, roleName);
        publisher.publishEvent(new NewUserEvent(userDTO.getEmail(), ROLE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userDTO.getId()),
                                    "compte", userService.getDetailCompte(idCompte),
                                    "events", userEventsService.getEventsByUserId(idCompte),
                                    "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Role updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/settings")
    public ResponseEntity<HttpResponse> updateAccountSettings(Authentication authentication, @RequestBody @Valid SettingsForm form) {
        UserDto userResponse = getAuthenticatedUser(authentication);
        userService.updateAccountSettings(userResponse.getId(), form.getEnabled(), form.getNotLocked());
        publisher.publishEvent(new NewUserEvent(userResponse.getEmail(), ACCOUNT_SETTINGS_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userResponse.getId()), "events", userEventsService.getEventsByUserId(userResponse.getId()), "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Account settings updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/settingsByAdmin")
    public ResponseEntity<HttpResponse> updateAccountSettingsByAdmin(Authentication authentication, @RequestBody @Valid SettingsForm form) {
        UserDto userResponse = getAuthenticatedUser(authentication);
        userService.updateAccountSettingsByAdmin(form.getId(), form.getEnabled(), form.getNotLocked());
        publisher.publishEvent(new NewUserEvent(userResponse.getEmail(), ACCOUNT_SETTINGS_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(userResponse.getId()),
                                    "compte", userService.getDetailCompte(form.getId()),
                                    "events", userEventsService.getEventsByUserId(form.getId()),
                                    "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Account updated successfully")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/togglemfa")
    public ResponseEntity<HttpResponse> toggleMfa(Authentication authentication) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        UserDto user = userService.toggleMfa(getAuthenticatedUser(authentication).getEmail());
        publisher.publishEvent(new NewUserEvent(user.getEmail(), MFA_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", user, "events", userEventsService.getEventsByUserId(user.getId()), "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Multi-Factor Authentication updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @PatchMapping("/update/image")
    public ResponseEntity<HttpResponse> updateProfileImage(Authentication authentication, @RequestParam("image") MultipartFile image) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        UserDto user = getAuthenticatedUser(authentication);
        userService.updateImage(user, image);
        publisher.publishEvent(new NewUserEvent(user.getEmail(), PROFILE_PICTURE_UPDATE));
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .data(Map.of("user", userService.getUserById(user.getId()), "events", userEventsService.getEventsByUserId(user.getId()), "roles", rolesService.getRoles()))
                        .timeStamp(now().toString())
                        .message("Profile image updated")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping(value = "/image/{fileName}", produces = IMAGE_PNG_VALUE)
    public byte[] getProfileImage(@PathVariable("fileName") String fileName) throws Exception {
        return Files.readAllBytes(Paths.get(System.getProperty("user.home") + "/IdeaProjects/securecapita/src/main/resources/imagesProfiles/" + fileName));
    }

    private ResponseEntity<HttpResponse> sendVerificationCode(UserDto userDto){
        System.out.println("Send Verification code");
        userService.sendVerificationCode(userDto);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDto))
                        .message("Verification Code Sent")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private ResponseEntity<HttpResponse> sendResponse(UserDto userDto){
        System.out.println("Send response ");
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", userDto, "access_token",jwtTokenProvider.createAccessToken(getUserPrincipal(userDto))
                                , "refresh_token", jwtTokenProvider.createRefreshToken(getUserPrincipal(userDto))))
                        .message("Login Success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private UserPrincipal getUserPrincipal(UserDto user) {
        UserDto userResponse = userService.getUserByEmail(user.getEmail());
        UserDto userDto=mapper.map(userResponse,UserDto.class);
        return new UserPrincipal(mapper.map(userDto, User.class), mapper.map(rolesService.getRoleByUserId(userResponse.getId()), Role.class));
    }

    @GetMapping("/refresh/token")
    public ResponseEntity<HttpResponse> refreshToken(HttpServletRequest request) {
        if(isHeaderAndTokenValid(request)) {
            String token = request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length());
            UserDto user = userService.getUserById(jwtTokenProvider.getSubject(token, request));
            return ResponseEntity.ok().body(
                    HttpResponse.builder()
                            .timeStamp(now().toString())
                            .data(Map.of("user", user, "access_token", jwtTokenProvider.createAccessToken(getUserPrincipal(user))
                                    , "refresh_token", token))
                            .message("Token refreshed")
                            .status(OK)
                            .statusCode(OK.value())
                            .build());
        } else {
            return ResponseEntity.badRequest().body(
                    HttpResponse.builder()
                            .timeStamp(now().toString())
                            .reason("Refresh Token missing or invalid")
                            .developerMessage("Refresh Token missing or invalid")
                            .status(BAD_REQUEST)
                            .statusCode(BAD_REQUEST.value())
                            .build());
        }
    }

    private boolean isHeaderAndTokenValid(HttpServletRequest request) {
        return  request.getHeader(AUTHORIZATION) != null
                &&  request.getHeader(AUTHORIZATION).startsWith(TOKEN_PREFIX)
                && jwtTokenProvider.isTokenValid(
                jwtTokenProvider.getSubject(request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length()), request),
                request.getHeader(AUTHORIZATION).substring(TOKEN_PREFIX.length())
        );
    }

    @RequestMapping("/error")
    public ResponseEntity<HttpResponse> handleError(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason("There is no mapping for a " + request.getMethod() + " request for this path on the server")
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build());
    }

    private URI getUri() {
        return URI.create(fromCurrentContextPath().path("/secureapi/get/<userId>").toUriString());
    }


}

