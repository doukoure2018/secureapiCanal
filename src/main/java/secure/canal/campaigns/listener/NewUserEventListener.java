package secure.canal.campaigns.listener;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import secure.canal.campaigns.event.NewUserEvent;
import secure.canal.campaigns.service.UserEventService;

import java.time.LocalDateTime;

import static secure.canal.campaigns.utils.RequestUtils.getDevice;
import static secure.canal.campaigns.utils.RequestUtils.getIpAddress;

@Component
@RequiredArgsConstructor
public class NewUserEventListener {
    private final UserEventService eventService;
    private final HttpServletRequest request;

    @EventListener
    public void onNewUserEvent(NewUserEvent event) {
        eventService.addUserEvent(event.getEmail(), event.getType(), getDevice(request), getIpAddress(request), LocalDateTime.now());
    }
}
