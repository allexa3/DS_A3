package ro.tuc.ds.device.messaging;

/** Synchronization message published by the Device service. eventType = CREATED | UPDATED | DELETED. */
public record DeviceSyncEvent(String eventType, String deviceId, String name, double maxConsumption, String userId) {
}
