package secure.canal.campaigns.service.Impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.entity.Event;
import secure.canal.campaigns.enumeration.EventType;
import secure.canal.campaigns.repository.EventRepository;
import secure.canal.campaigns.repository.UserEventRepository;
import secure.canal.campaigns.repository.UserRepository;
import secure.canal.campaigns.service.EventService;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventsRepository;
    private final ModelMapper mapper;
    @Override
    public void addEvents(EventType eventType) {
        Event events= mapper.map(eventType,Event.class);
        eventsRepository.save(events);
    }
}
