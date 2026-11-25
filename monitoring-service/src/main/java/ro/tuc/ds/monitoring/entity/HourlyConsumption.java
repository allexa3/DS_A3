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

/** Total energy consumed by a device during one hour (hour_start is truncated to the full hour, UTC). */
@Entity
@Table(name = "hourly_consumption",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "hour_start"}))
public class HourlyConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "hour_start", nullable = false)
    private Instant hourStart;

    @Column(name = "total_kwh", nullable = false)
    private double totalKwh;

    public HourlyConsumption() {
    }

    public HourlyConsumption(UUID deviceId, Instant hourStart, double totalKwh) {
        this.deviceId = deviceId;
        this.hourStart = hourStart;
        this.totalKwh = totalKwh;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public Instant getHourStart() { return hourStart; }
    public double getTotalKwh() { return totalKwh; }
    public void setTotalKwh(double totalKwh) { this.totalKwh = totalKwh; }
}
