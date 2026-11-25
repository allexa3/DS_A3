package ro.tuc.ds.monitoring.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds.monitoring.config.Json;
import ro.tuc.ds.monitoring.config.RabbitConfig;
import ro.tuc.ds.monitoring.dto.DeviceSyncEvent;
import ro.tuc.ds.monitoring.service.SyncService;

import java.nio.charset.StandardCharsets;

/** Consumes device.* events from the Synchronization Broker. */
@Component
public class DeviceSyncConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSyncConsumer.class);

    private final SyncService service;

    public DeviceSyncConsumer(SyncService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbit.sync.queue}", containerFactory = RabbitConfig.SYNC_LISTENER_FACTORY)
    public void onDeviceEvent(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        LOGGER.info("[sync-broker] received {}: {}", message.getMessageProperties().getReceivedRoutingKey(), body);
        try {
            service.apply(Json.read(body, DeviceSyncEvent.class));
        } catch (RuntimeException e) {
            throw new AmqpRejectAndDontRequeueException("Invalid device event: " + e.getMessage(), e);
        }
    }
}
