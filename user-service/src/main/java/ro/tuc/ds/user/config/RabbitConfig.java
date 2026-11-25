package ro.tuc.ds.user.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Synchronization Broker topology. Every synchronization event goes to the durable topic exchange
 * {@code sync.events} with the routing key {@code user.created|updated|deleted} or {@code device.*};
 * each interested service binds its own queue (publish/subscribe), so every consumer gets its own copy.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange syncExchange(@Value("${app.rabbit.sync.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }
}
