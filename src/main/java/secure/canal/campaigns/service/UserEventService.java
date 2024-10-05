package secure.canal.campaigns.service;

import org.springframework.stereotype.Service;
import secure.canal.campaigns.enumeration.EventType;
import secure.canal.campaigns.payload.UserEventDto;

import java.time.LocalDateTime;
import java.util.List;


public interface UserEventService {
    List<UserEventDto> getEventsByUserId(Long userId);
    void addUserEvent(String email, EventType eventType, String device, String ipAddress, LocalDateTime createdAt);
}
