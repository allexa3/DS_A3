package ro.tuc.ds.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds.monitoring.entity.Measurement;

import java.time.Instant;
import java.util.UUID;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

    boolean existsByDeviceIdAndMeasuredAt(UUID deviceId, Instant measuredAt);

    void deleteByDeviceId(UUID deviceId);
}
