package ro.tuc.ds.device.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds.device.entity.Device;
import ro.tuc.ds.device.repository.DeviceRepository;

/** Re-publishes every existing device at startup (consumers upsert, so this is idempotent). */
@Component
public class DeviceResyncOnStartup {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResyncOnStartup.class);

    private final DeviceRepository repository;
    private final DeviceEventPublisher publisher;

    public DeviceResyncOnStartup(DeviceRepository repository, DeviceEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resync() {
        Thread t = new Thread(() -> {
            try {
                int n = 0;
                for (Device d : repository.findAll()) {
                    publisher.created(d);
                    n++;
                }
                LOGGER.info("[sync-broker] resynchronized {} existing devices", n);
            } catch (RuntimeException e) {
                LOGGER.warn("[sync-broker] device resync skipped: {}", e.getMessage());
            }
        }, "device-resync");
        t.setDaemon(true);
        t.start();
    }
}
