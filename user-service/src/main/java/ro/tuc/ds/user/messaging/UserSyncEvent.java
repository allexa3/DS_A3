package ro.tuc.ds.user.messaging;

/**
 * Synchronization message for a user. eventType = CREATED | UPDATED | DELETED.
 * The password never leaves this service.
 */
public record UserSyncEvent(String eventType, String userId, String username, String role) {
}
