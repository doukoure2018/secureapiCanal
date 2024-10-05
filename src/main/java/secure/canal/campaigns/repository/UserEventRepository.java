package secure.canal.campaigns.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import secure.canal.campaigns.entity.UserEvent;
import secure.canal.campaigns.payload.UserEventDto;

import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    @Query("SELECT new secure.canal.campaigns.payload.UserEventDto(uev.id, uev.device, uev.ipAddress, ev.type, ev.description, uev.createdAt) " +
            "FROM UserEvent uev JOIN uev.event ev JOIN uev.user u " +
            "WHERE u.id = :userId " +
            "ORDER BY uev.createdAt DESC LIMIT 10")
    List<UserEventDto> findRecentEventsByUserId(@Param("userId") Long userId);

}