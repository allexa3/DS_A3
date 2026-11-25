package ro.tuc.ds.device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds.device.entity.UserRef;

import java.util.UUID;

public interface UserRefRepository extends JpaRepository<UserRef, UUID> {
}
