package ro.tuc.ds.user.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ro.tuc.ds.user.config.Json;
import ro.tuc.ds.user.entity.User;

/** Publishes user synchronization events on the Synchronization Broker. */
@Component
public class UserEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final TopicExchange exchange;

    public UserEventPublisher(RabbitTemplate rabbit, TopicExchange syncExchange) {
        this.rabbit = rabbit;
        this.exchange = syncExchange;
    }

    public void created(User u) {
        publish("CREATED", u);
    }

    public void updated(User u) {
        publish("UPDATED", u);
    }

    public void deleted(User u) {
        publish("DELETED", u);
    }

    private void publish(String type, User u) {
        String body = Json.write(new UserSyncEvent(type, u.getId().toString(), u.getUsername(), u.getRole().name()));
        String routingKey = "user." + type.toLowerCase();
        try {
            rabbit.convertAndSend(exchange.getName(), routingKey, body);
            LOGGER.info("[sync-broker] published {}: {}", routingKey, body);
        } catch (AmqpException e) {
            // The user is already saved; the full resync at the next start repairs a lost message.
            LOGGER.error("[sync-broker] could not publish {} for user {}: {}", routingKey, u.getId(), e.getMessage());
        }
    }
}
