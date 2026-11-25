package ro.tuc.ds.monitoring.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.tuc.ds.monitoring.dto.HourlyConsumptionResponse;
import ro.tuc.ds.monitoring.exception.ForbiddenException;
import ro.tuc.ds.monitoring.service.ConsumptionQueryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final ConsumptionQueryService service;

    public MonitoringController(ConsumptionQueryService service) {
        this.service = service;
    }

    @Operation(summary = "Hourly energy consumption of a device for one day",
            description = "date = yyyy-MM-dd, tz = IANA time zone (default UTC). Returns 24 values (hour 0-23) in kWh. "
                    + "X-User-Id / X-User-Role are added by the gateway; a CLIENT only gets its own devices (403 otherwise).")
    @GetMapping("/devices/{deviceId}/hourly")
    public HourlyConsumptionResponse hourly(@PathVariable UUID deviceId,
                                            @RequestParam String date,
                                            @RequestParam(defaultValue = "UTC") String tz,
                                            @RequestHeader(value = "X-User-Id", required = false) String userId,
                                            @RequestHeader(value = "X-User-Role", required = false) String role) {
        UUID caller;
        try {
            caller = userId == null ? null : UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Invalid caller identity");
        }
        return service.hourlyOfDay(deviceId, date, tz, role, caller);
    }
}
