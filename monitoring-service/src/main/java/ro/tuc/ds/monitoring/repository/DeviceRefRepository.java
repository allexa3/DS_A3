package ro.tuc.ds.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds.monitoring.entity.DeviceRef;

import java.util.UUID;

public interface DeviceRefRepository extends JpaRepository<DeviceRef, UUID> {
}
