package ro.tuc.ds.simulator;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

/**
 * Energy consumption of ONE smart meter over a 10-minute interval, in kWh.
 * <p>
 * Every meter starts from its own random base load; the base load is multiplied by a daily profile
 * (low at night, a morning bump, flat during the day, the highest values in the evening) and a small random
 * fluctuation, with an occasional short spike (an appliance switched on).
 */
final class ConsumptionModel {

    /** {hour of day, factor}: factors are linearly interpolated between these points. */
    private static final double[][] PROFILE = {
            {0, 0.50}, {5, 0.45}, {7, 1.10}, {9, 0.90}, {12, 0.85},
            {17, 1.00}, {19, 1.70}, {21, 1.50}, {23, 0.80}, {24, 0.50}};

    private final Random random;
    /** kWh per 10 minutes at factor 1.0 (0.04 - 0.12 kWh = 0.24 - 0.72 kW average). */
    private final double baseLoad;

    ConsumptionModel(Random random) {
        this.random = random;
        this.baseLoad = 0.04 + random.nextDouble() * 0.08;
    }

    double next(java.time.Instant at) {
        ZonedDateTime local = at.atZone(ZoneId.systemDefault());
        double hour = local.getHour() + local.getMinute() / 60.0;
        double fluctuation = 1.0 + (random.nextDouble() - 0.5) * 0.20;       // +-10 %
        double spike = random.nextDouble() < 0.05 ? 1.5 : 1.0;               // 5 % of the readings
        double value = baseLoad * profile(hour) * fluctuation * spike;
        return Math.round(value * 10000.0) / 10000.0;
    }

    static double profile(double hour) {
        for (int i = 1; i < PROFILE.length; i++) {
            if (hour <= PROFILE[i][0]) {
                double h0 = PROFILE[i - 1][0];
                double h1 = PROFILE[i][0];
                double t = (hour - h0) / (h1 - h0);
                return PROFILE[i - 1][1] + t * (PROFILE[i][1] - PROFILE[i - 1][1]);
            }
        }
        return PROFILE[PROFILE.length - 1][1];
    }
}
