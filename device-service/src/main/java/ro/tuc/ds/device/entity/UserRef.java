package ro.tuc.ds.device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Local copy of a user (id + a few attributes), filled from the synchronization queue (user.* events). */
@Entity
@Table(name = "user_refs")
public class UserRef {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private String username;

    private String role;

    public UserRef() {
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
