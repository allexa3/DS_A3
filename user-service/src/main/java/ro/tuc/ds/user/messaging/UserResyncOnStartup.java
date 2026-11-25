package ro.tuc.ds.user.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds.user.entity.User;
import ro.tuc.ds.user.repository.UserRepository;

/**
 * Re-publishes every existing user at startup. Consumers upsert, so this is idempotent; it fills the other
 * services' local copies after a restore from a database dump or when a message was lost while a broker was down.
 */
@Component
public class UserResyncOnStartup {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserResyncOnStartup.class);

    private final UserRepository repository;
    private final UserEventPublisher publisher;

    public UserResyncOnStartup(UserRepository repository, UserEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resync() {
        Thread t = new Thread(() -> {
            try {
                int n = 0;
                for (User u : repository.findAll()) {
                    publisher.created(u);
                    n++;
                }
                LOGGER.info("[sync-broker] resynchronized {} existing users", n);
            } catch (RuntimeException e) {
                LOGGER.warn("[sync-broker] user resync skipped: {}", e.getMessage());
            }
        }, "user-resync");
        t.setDaemon(true);
        t.start();
    }
}
