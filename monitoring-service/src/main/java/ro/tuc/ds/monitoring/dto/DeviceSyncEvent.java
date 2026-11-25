package ro.tuc.ds.monitoring.dto;

/** Synchronization event published by the Device service. eventType is CREATED, UPDATED or DELETED. */
public record DeviceSyncEvent(String eventType, String deviceId, String name, double maxConsumption, String userId) {
}
