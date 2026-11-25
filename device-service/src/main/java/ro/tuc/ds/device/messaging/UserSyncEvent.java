package ro.tuc.ds.device.messaging;

/** Synchronization message published by the User service. eventType = CREATED | UPDATED | DELETED. */
public record UserSyncEvent(String eventType, String userId, String username, String role) {
}
