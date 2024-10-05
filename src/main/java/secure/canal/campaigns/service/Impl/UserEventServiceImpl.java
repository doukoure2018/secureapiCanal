package secure.canal.campaigns.service.Impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.entity.Event;
import secure.canal.campaigns.entity.User;
import secure.canal.campaigns.entity.UserEvent;
import secure.canal.campaigns.enumeration.EventType;
import secure.canal.campaigns.payload.UserEventDto;
import secure.canal.campaigns.repository.EventRepository;
import secure.canal.campaigns.repository.UserEventRepository;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.service.UserEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserEventServiceImpl implements UserEventService {

    private final UserEventRepository userEventsRepository;
    private final UserRepository userRepository;
    private final EventRepository eventsRepository;

    @Override
    public List<UserEventDto> getEventsByUserId(Long userId) {
        return userEventsRepository.findRecentEventsByUserId(userId);
    }

    @Override
    public void addUserEvent(String email, EventType eventType, String device, String ipAddress, LocalDateTime createdAt) {
        // get User by email
        Optional<User> user = userRepository.findByEmail(email);
        // get Event by eventType
        Optional<Event> events = eventsRepository.findByType(eventType.toString());
        UserEvent userEvents = new UserEvent();
        userEvents.setUser(user.get());
        userEvents.setEvent(events.get());
        userEvents.setDevice(device);
        userEvents.setIpAddress(ipAddress);
        userEvents.setCreatedAt(LocalDateTime.now());
        userEventsRepository.save(userEvents);
    }
}
