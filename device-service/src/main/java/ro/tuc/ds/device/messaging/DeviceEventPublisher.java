package ro.tuc.ds.device.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ro.tuc.ds.device.config.Json;
import ro.tuc.ds.device.entity.Device;

/** Publishes device synchronization events on the Synchronization Broker (consumed by the Monitoring service). */
@Component
public class DeviceEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final TopicExchange exchange;

    public DeviceEventPublisher(RabbitTemplate rabbit, TopicExchange syncExchange) {
        this.rabbit = rabbit;
        this.exchange = syncExchange;
    }

    public void created(Device d) {
        publish("CREATED", d);
    }

    /** Also used when a device is assigned to / unassigned from a user. */
    public void updated(Device d) {
        publish("UPDATED", d);
    }

    public void deleted(Device d) {
        publish("DELETED", d);
    }

    private void publish(String type, Device d) {
        String body = Json.write(new DeviceSyncEvent(type, d.getId().toString(), d.getName(), d.getMaxConsumption(),
                d.getUserId() == null ? null : d.getUserId().toString()));
        String routingKey = "device." + type.toLowerCase();
        try {
            rabbit.convertAndSend(exchange.getName(), routingKey, body);
            LOGGER.info("[sync-broker] published {}: {}", routingKey, body);
        } catch (AmqpException e) {
            LOGGER.error("[sync-broker] could not publish {} for device {}: {}", routingKey, d.getId(), e.getMessage());
        }
    }
}
