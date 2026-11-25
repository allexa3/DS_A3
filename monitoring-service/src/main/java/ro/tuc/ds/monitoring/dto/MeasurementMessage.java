package ro.tuc.ds.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent by the Device Data Simulator:
 * {"timestamp":"2026-11-17T10:10:00Z","device_id":"<uuid>","measurement_value":0.153}.
 * measurement_value is the energy consumed during the 10-minute interval, in kWh.
 */
public record MeasurementMessage(
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("measurement_value") Double measurementValue) {
}
