package ro.tuc.ds.device.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Synchronization Broker topology: the Device service publishes {@code device.*} events to the topic exchange and
 * owns the queue {@code sync.device-service.users}, bound with {@code user.*} (every user event is copied to it).
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange syncExchange(@Value("${app.rabbit.sync.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public Queue userSyncQueue(@Value("${app.rabbit.sync.queue}") String name) {
        return new Queue(name, true);
    }

    @Bean
    public Binding userSyncBinding(Queue userSyncQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(userSyncQueue).to(syncExchange).with("user.*");
    }
}
