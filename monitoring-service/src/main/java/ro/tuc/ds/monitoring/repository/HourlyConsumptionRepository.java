package ro.tuc.ds.monitoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds.monitoring.entity.HourlyConsumption;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, UUID> {

    Optional<HourlyConsumption> findByDeviceIdAndHourStart(UUID deviceId, Instant hourStart);

    List<HourlyConsumption> findByDeviceIdAndHourStartGreaterThanEqualAndHourStartLessThanOrderByHourStart(
            UUID deviceId, Instant from, Instant to);

    void deleteByDeviceId(UUID deviceId);
}
