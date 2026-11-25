package ro.tuc.ds.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Local copy of the device data this service needs. Filled from the synchronization queue (device.* events). */
@Entity
@Table(name = "device_refs")
public class DeviceRef {

    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "user_id")
    private UUID userId;

    private String name;

    @Column(name = "max_consumption")
    private double maxConsumption;

    public DeviceRef() {
    }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getMaxConsumption() { return maxConsumption; }
    public void setMaxConsumption(double maxConsumption) { this.maxConsumption = maxConsumption; }
}
