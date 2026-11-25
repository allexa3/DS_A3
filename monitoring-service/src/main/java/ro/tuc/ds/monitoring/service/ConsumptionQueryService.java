package ro.tuc.ds.monitoring.service;

import org.springframework.stereotype.Service;
import ro.tuc.ds.monitoring.dto.HourlyConsumptionResponse;
import ro.tuc.ds.monitoring.entity.DeviceRef;
import ro.tuc.ds.monitoring.entity.HourlyConsumption;
import ro.tuc.ds.monitoring.exception.BadRequestException;
import ro.tuc.ds.monitoring.exception.ForbiddenException;
import ro.tuc.ds.monitoring.exception.ResourceNotFoundException;
import ro.tuc.ds.monitoring.repository.DeviceRefRepository;
import ro.tuc.ds.monitoring.repository.HourlyConsumptionRepository;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConsumptionQueryService {

    private final DeviceRefRepository devices;
    private final HourlyConsumpti7onRepository hourly;

    public ConsumptionQueryService(DeviceRefRepository devices, HourlyConsumptionRepository hourly) {
        this.devices = devices;
        this.hourly = hourly;
    }

    /**
     * @param role   role of the caller, set by the gateway after validating the token (X-User-Role)
     * @param userId id of the caller, set by the gateway (X-User-Id)
     */
    public HourlyConsumptionResponse hourlyOfDay(UUID deviceId, String date, String timeZone, String role, UUID userId) {
        DeviceRef device = devices.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device with id " + deviceId + " not found"));
        authorize(device, role, userId);

        LocalDate day;
        ZoneId zone;
        try {
            day = LocalDate.parse(date);
            zone = ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            throw new BadRequestException("date must be yyyy-MM-dd and tz a valid time zone id, e.g. Europe/Bucharest");
        }
        ZonedDateTime start = day.atStartOfDay(zone);
        ZonedDateTime end = day.plusDays(1).atStartOfDay(zone);

        double[] perHour = new double[24];
        List<HourlyConsumption> rows = hourly
                .findByDeviceIdAndHourStartGreaterThanEqualAndHourStartLessThanOrderByHourStart(
                        deviceId, start.toInstant(), end.toInstant());
        for (HourlyConsumption row : rows) {
            perHour[row.getHourStart().atZone(zone).getHour()] += row.getTotalKwh();
        }
        List<HourlyConsumptionResponse.Hour> hours = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            hours.add(new HourlyConsumptionResponse.Hour(h, Math.round(perHour[h] * 10000.0) / 10000.0));
        }
        return new HourlyConsumptionResponse(deviceId.toString(), day.toString(), zone.getId(), "kWh", hours);
    }

    /** A client may only read the devices assigned to him/her; an administrator may read every device. */
    private static void authorize(DeviceRef device, String role, UUID userId) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if ("CLIENT".equalsIgnoreCase(role) && userId != null && userId.equals(device.getUserId())) {
            return;
        }
        throw new ForbiddenException("You are not allowed to see the consumption of this device");
    }
}
