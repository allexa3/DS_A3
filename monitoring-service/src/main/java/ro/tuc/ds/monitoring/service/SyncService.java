package ro.tuc.ds.monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds.monitoring.dto.DeviceSyncEvent;
import ro.tuc.ds.monitoring.entity.DeviceRef;
import ro.tuc.ds.monitoring.repository.DeviceRefRepository;
import ro.tuc.ds.monitoring.repository.HourlyConsumptionRepository;
import ro.tuc.ds.monitoring.repository.MeasurementRepository;

import java.util.UUID;

/** Applies the device events received on the synchronization queue to the local Monitoring database. */
@Service
public class SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncService.class);

    private final DeviceRefRepository devices;
    private final MeasurementRepository measurements;
    private final HourlyConsumptionRepository hourly;

    public SyncService(DeviceRefRepository devices, MeasurementRepository measurements, HourlyConsumptionRepository hourly) {
        this.devices = devices;
        this.measurements = measurements;
        this.hourly = hourly;
    }

    @Transactional
    public void apply(DeviceSyncEvent event) {
        UUID id = UUID.fromString(event.deviceId());
        if ("DELETED".equalsIgnoreCase(event.eventType())) {
            devices.deleteById(id);
            measurements.deleteByDeviceId(id);
            hourly.deleteByDeviceId(id);
            LOGGER.info("[sync] device {} deleted, its measurements were removed", id);
            return;
        }
        // CREATED and UPDATED are the same operation: upsert (idempotent, so redelivery and full resync are harmless)
        DeviceRef ref = devices.findById(id).orElseGet(DeviceRef::new);
        ref.setDeviceId(id);
        ref.setName(event.name());
        ref.setMaxConsumption(event.maxConsumption());
        ref.setUserId(event.userId() == null || event.userId().isBlank() ? null : UUID.fromString(event.userId()));
        devices.save(ref);
        LOGGER.info("[sync] device {} {} (owner {})", id, event.eventType(), ref.getUserId());
    }
}
