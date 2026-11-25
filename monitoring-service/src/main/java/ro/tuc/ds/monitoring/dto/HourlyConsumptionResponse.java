package ro.tuc.ds.monitoring.dto;

import java.util.List;

/** Energy consumed by one device during every hour (0-23, in the requested time zone) of one day. */
public record HourlyConsumptionResponse(String deviceId, String date, String timeZone, String unit, List<Hour> hours) {

    public record Hour(int hour, double kwh) {
    }
}
