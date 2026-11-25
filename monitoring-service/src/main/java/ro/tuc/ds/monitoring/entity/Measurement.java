package ro.tuc.ds.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** One raw 10-minute reading. The unique key makes message redelivery harmless (no double counting). */
@Entity
@Table(name = "measurements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "measured_at"}))
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    /** Energy consumed during the 10-minute interval, in kWh. */
    @Column(name = "value_kwh", nullable = false)
    private double valueKwh;

    public Measurement() {
    }

    public Measurement(UUID deviceId, Instant measuredAt, double valueKwh) {
        this.deviceId = deviceId;
        this.measuredAt = measuredAt;
        this.valueKwh = valueKwh;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public Instant getMeasuredAt() { return measuredAt; }
    public double getValueKwh() { return valueKwh; }
}
