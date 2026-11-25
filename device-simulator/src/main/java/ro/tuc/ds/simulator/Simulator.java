package ro.tuc.ds.simulator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Generates one reading per device every 10 simulated minutes. The simulated clock advances 10 minutes per tick;
 * the real time between two ticks is configurable (600 s = real time, 5 s = fast demo).
 */
final class Simulator {

    static final Duration STEP = Duration.ofMinutes(10);

    record Config(String host, int port, String user, String password, String queue,
                  List<UUID> devices, int intervalSeconds, boolean backfillToday) {
    }

    private final Consumer<String> log;
    private ScheduledExecutorService scheduler;
    private MeasurementPublisher publisher;
    private final Map<UUID, ConsumptionModel> models = new LinkedHashMap<>();
    private Instant cursor;

    Simulator(Consumer<String> log) {
        this.log = log;
    }

    synchronized boolean isRunning() {
        return scheduler != null;
    }

    synchronized void start(Config cfg) throws Exception {
        if (isRunning()) {
            return;
        }
        publisher = new MeasurementPublisher(cfg.host(), cfg.port(), cfg.user(), cfg.password(), cfg.queue());
        Random random = new Random();
        models.clear();
        for (UUID device : cfg.devices()) {
            models.put(device, new ConsumptionModel(random));
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        cursor = now.minusSeconds(now.atZone(ZoneId.systemDefault()).getMinute() % 10 * 60L);   // floor to 10 min
        log.accept("Connected to " + cfg.host() + ":" + cfg.port() + ", queue '" + cfg.queue() + "', "
                + models.size() + " device(s)");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "simulator");
            t.setDaemon(true);
            return t;
        });
        if (cfg.backfillToday()) {
            scheduler.execute(this::backfill);
        }
        ScheduledFuture<?> ignored = scheduler.scheduleWithFixedDelay(this::tick,
                cfg.backfillToday() ? 1 : 0, Math.max(1, cfg.intervalSeconds()), TimeUnit.SECONDS);
    }

    synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
        log.accept("Stopped");
    }

    /** Sends every missing reading from 00:00 (local time) today up to now, at once. */
    private void backfill() {
        try {
            Instant end = cursor;
            Instant t = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
            int n = 0;
            for (; t.isBefore(end); t = t.plus(STEP)) {
                sendAll(t, false);
                n++;
            }
            log.accept("Backfill done: " + n + " intervals sent for today so far");
        } catch (Exception e) {
            log.accept("Backfill failed: " + e.getMessage());
        }
    }

    private void tick() {
        try {
            sendAll(cursor, true);
            cursor = cursor.plus(STEP);
        } catch (Exception e) {
            log.accept("Send failed: " + e.getMessage());
        }
    }

    private void sendAll(Instant timestamp, boolean verbose) throws Exception {
        for (Map.Entry<UUID, ConsumptionModel> e : models.entrySet()) {
            double value = e.getValue().next(timestamp);
            String json = publisher.publish(timestamp, e.getKey(), value);
            if (verbose) {
                log.accept("Sent " + json);
            }
        }
    }
}
