package ro.tuc.ds.device.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds.device.client.UserClient;
import ro.tuc.ds.device.dto.DeviceRequest;
import ro.tuc.ds.device.dto.DeviceResponse;
import ro.tuc.ds.device.entity.Device;
import ro.tuc.ds.device.exception.ResourceNotFoundException;
import ro.tuc.ds.device.messaging.DeviceEventPublisher;
import ro.tuc.ds.device.repository.UserRefRepository;
import ro.tuc.ds.device.repository.DeviceRepository;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository repository;
    private final UserClient userClient;
    private final UserRefRepository userRefs;
    private final DeviceEventPublisher events;

    public DeviceService(DeviceRepository repository, UserClient userClient, UserRefRepository userRefs,
                         DeviceEventPublisher events) {
        this.repository = repository;
        this.userClient = userClient;
        this.userRefs = userRefs;
        this.events = events;
    }

    public List<DeviceResponse> findAll() {
        return repository.findAll().stream().map(DeviceResponse::from).toList();
    }

    public DeviceResponse findById(UUID id) {
        return DeviceResponse.from(getOrThrow(id));
    }

    public List<DeviceResponse> findByUser(UUID userId) {
        return repository.findByUserId(userId).stream().map(DeviceResponse::from).toList();
    }

    public DeviceResponse create(DeviceRequest req) {
        Device d = new Device();
        apply(d, req);
        d = repository.save(d);
        events.created(d);   // synchronization queue: Monitoring service stores the new device
        LOGGER.debug("Device {} created", d.getId());
        return DeviceResponse.from(d);
    }

    public DeviceResponse update(UUID id, DeviceRequest req) {
        Device d = getOrThrow(id);
        apply(d, req);
        d = repository.save(d);
        events.updated(d);
        return DeviceResponse.from(d);
    }

    public void delete(UUID id) {
        Device d = getOrThrow(id);
        repository.deleteById(id);
        events.deleted(d);
    }

    public DeviceResponse assign(UUID deviceId, UUID userId) {
        Device d = getOrThrow(deviceId);
        // The user id was copied here by the synchronization queue; ask the User service only if the event has not
        // arrived yet (e.g. the user was created a moment ago).
        if (!userRefs.existsById(userId) && !userClient.exists(userId)) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }
        d.setUserId(userId);
        d = repository.save(d);
        events.updated(d);   // the Monitoring service needs the new owner to authorize reads
        return DeviceResponse.from(d);
    }

    public DeviceResponse unassign(UUID deviceId) {
        Device d = getOrThrow(deviceId);
        d.setUserId(null);
        d = repository.save(d);
        events.updated(d);
        return DeviceResponse.from(d);
    }

    @Transactional
    public void unassignAllOfUser(UUID userId) {
        List<Device> owned = repository.findByUserId(userId);
        for (Device d : owned) {
            d.setUserId(null);
            repository.save(d);
            events.updated(d);
        }
        LOGGER.debug("Unassigned {} devices from user {}", owned.size(), userId);
    }

    private void apply(Device d, DeviceRequest req) {
        d.setName(req.name());
        d.setDescription(req.description());
        d.setMaxConsumption(req.maxConsumption());
    }

    private Device getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device with id " + id + " not found"));
    }
}
