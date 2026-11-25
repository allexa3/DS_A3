package ro.tuc.ds.monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds.monitoring.dto.MeasurementMessage;
import ro.tuc.ds.monitoring.entity.HourlyConsumption;
import ro.tuc.ds.monitoring.entity.Measurement;
import ro.tuc.ds.monitoring.repository.DeviceRefRepository;
import ro.tuc.ds.monitoring.repository.HourlyConsumptionRepository;
import ro.tuc.ds.monitoring.repository.MeasurementRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Turns 10-minute readings into hourly totals. */
@Service
public class MeasurementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementService.class);

    private final DeviceRefRepository devices;
    private final MeasurementRepository measurements;
    private final HourlyConsumptionRepository hourly;

    public MeasurementService(DeviceRefRepository devices, MeasurementRepository measurements,
                              HourlyConsumptionRepository hourly) {
        this.devices = devices;
        this.measurements = measurements;
        this.hourly = hourly;
    }

    /**
     * Stores the raw reading and adds it to the total of its hour (full hour, UTC).
     * A reading that was already processed (same device and timestamp) is ignored, so a redelivered
     * message never counts twice. Readings of devices that were never synchronized are dropped.
     *
     * @throws IllegalArgumentException if the message is malformed (the caller rejects it)
     */
    @Transactional
    public void process(MeasurementMessage m) {
        if (m.deviceId() == null || m.timestamp() == null || m.measurementValue() == null) {
            throw new IllegalArgumentException("timestamp, device_id and measurement_value are required");
        }
        UUID deviceId = UUID.fromString(m.deviceId());
        Instant at = Instant.parse(m.timestamp());
        double value = m.measurementValue();
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("measurement_value must be a non-negative number");
        }
        if (!devices.existsById(deviceId)) {
            LOGGER.warn("[data] unknown device {} - reading dropped (create the device first)", deviceId);
            return;
        }
        if (measurements.existsByDeviceIdAndMeasuredAt(deviceId, at)) {
            LOGGER.debug("[data] duplicate reading of {} at {} ignored", deviceId, at);
            return;
        }
        measurements.save(new Measurement(deviceId, at, value));

        Instant hourStart = at.truncatedTo(ChronoUnit.HOURS);
        HourlyConsumption h = hourly.findByDeviceIdAndHourStart(deviceId, hourStart)
                .orElseGet(() -> new HourlyConsumption(deviceId, hourStart, 0));
        h.setTotalKwh(h.getTotalKwh() + value);
        hourly.save(h);
        LOGGER.info("[data] device {} hour {} total = {} kWh", deviceId, hourStart, String.format("%.4f", h.getTotalKwh()));
    }
}
