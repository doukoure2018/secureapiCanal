package secure.canal.campaigns.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import secure.canal.campaigns.entity.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event,Long> {

    Optional<Event> findByType(String eventType);
}
