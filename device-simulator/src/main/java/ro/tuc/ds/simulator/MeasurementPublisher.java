package ro.tuc.ds.simulator;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/** Sends measurements, as JSON, to a durable queue of the Data Collection Broker (point-to-point). */
final class MeasurementPublisher implements AutoCloseable {

    private final Connection connection;
    private final Channel channel;
    private final String queue;

    MeasurementPublisher(String host, int port, String user, String password, String queue)
            throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        factory.setAutomaticRecoveryEnabled(true);
        this.connection = factory.newConnection("device-data-simulator");
        this.channel = connection.createChannel();
        this.queue = queue;
        // same arguments as the consumer (durable, not exclusive, not auto-delete), so both declarations agree
        channel.queueDeclare(queue, true, false, false, null);
    }

    /** JSON of the form {"timestamp":"...","device_id":"...","measurement_value":0.1234}. */
    static String toJson(Instant timestamp, UUID deviceId, double value) {
        return String.format(Locale.ROOT,
                "{\"timestamp\":\"%s\",\"device_id\":\"%s\",\"measurement_value\":%.4f}", timestamp, deviceId, value);
    }

    synchronized String publish(Instant timestamp, UUID deviceId, double value) throws IOException {
        String json = toJson(timestamp, deviceId, value);
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)            // persistent: survives a broker restart
                .build();
        channel.basicPublish("", queue, props, json.getBytes(StandardCharsets.UTF_8));
        return json;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException | TimeoutException | RuntimeException ignored) {
            // already closed
        }
        try {
            connection.close();
        } catch (IOException | RuntimeException ignored) {
            // already closed
        }
    }
}
