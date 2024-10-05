package secure.canal.campaigns.utils;

import org.springframework.security.core.Authentication;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.payload.UserPrincipal;

public class UserUtils {
    public static UserDto getAuthenticatedUser(Authentication authentication){
         return ((UserDto) authentication.getPrincipal());
    }

    public static UserDto getLoggedInUser(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUser();
    }
}
