package ro.tuc.ds.device.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds.device.entity.UserRef;
import ro.tuc.ds.device.messaging.UserSyncEvent;
import ro.tuc.ds.device.repository.UserRefRepository;

import java.util.UUID;

/** Applies the user events received on the synchronization queue to the local Device database. */
@Service
public class UserSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncService.class);

    private final UserRefRepository users;
    private final DeviceService devices;

    public UserSyncService(UserRefRepository users, DeviceService devices) {
        this.users = users;
        this.devices = devices;
    }

    @Transactional
    public void apply(UserSyncEvent event) {
        UUID id = UUID.fromString(event.userId());
        if ("DELETED".equalsIgnoreCase(event.eventType())) {
            users.deleteById(id);
            devices.unassignAllOfUser(id);    // the owner is gone: the devices stay, but are unassigned
            LOGGER.info("[sync] user {} deleted, its devices were unassigned", id);
            return;
        }
        UserRef ref = users.findById(id).orElseGet(UserRef::new);   // CREATED / UPDATED = upsert (idempotent)
        ref.setUserId(id);
        ref.setUsername(event.username());
        ref.setRole(event.role());
        users.save(ref);
        LOGGER.info("[sync] user {} {} stored in the local database", id, event.eventType());
    }
}
