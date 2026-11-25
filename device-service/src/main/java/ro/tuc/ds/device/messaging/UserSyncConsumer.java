package ro.tuc.ds.device.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds.device.config.Json;
import ro.tuc.ds.device.service.UserSyncService;

import java.nio.charset.StandardCharsets;

/** Consumes the user.* events published by the User service. */
@Component
public class UserSyncConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncConsumer.class);

    private final UserSyncService service;

    public UserSyncConsumer(UserSyncService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbit.sync.queue}")
    public void onUserEvent(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        LOGGER.info("[sync-broker] received {}: {}", message.getMessageProperties().getReceivedRoutingKey(), body);
        try {
            service.apply(Json.read(body, UserSyncEvent.class));
        } catch (RuntimeException e) {
            throw new AmqpRejectAndDontRequeueException("Invalid user event: " + e.getMessage(), e);
        }
    }
}
