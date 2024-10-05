package secure.canal.campaigns.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import secure.canal.campaigns.enumeration.EventType;

@Getter
@Setter
public class NewUserEvent extends ApplicationEvent {
    private EventType type;
    private String email;

    public NewUserEvent(String email, EventType type) {
        super(email);
        this.type = type;
        this.email = email;
    }
}
