package ro.tuc.ds.monitoring.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * The Monitoring service talks to TWO different brokers (see the conceptual diagram of the assignment):
 * <ul>
 *   <li><b>Data Collection Broker</b> - queue {@code device-measurements}, point-to-point, written by the simulator.</li>
 *   <li><b>Synchronization Broker</b> - topic exchange {@code sync.events}; this service owns the queue
 *       {@code sync.monitoring-service.devices} bound with {@code device.*} (publish/subscribe: every service gets its own copy).</li>
 * </ul>
 * Each queue/exchange is declared only on the broker it belongs to ({@code setAdminsThatShouldDeclare}).
 */
@Configuration
public class RabbitConfig {

    public static final String DATA_LISTENER_FACTORY = "dataListenerFactory";
    public static final String SYNC_LISTENER_FACTORY = "syncListenerFactory";

    // ------------------------------------------------------------------ connections
    @Bean
    @Primary
    public CachingConnectionFactory dataConnectionFactory(@Value("${app.rabbit.data.host}") String host,
                                                          @Value("${app.rabbit.data.port}") int port,
                                                          @Value("${app.rabbit.data.username}") String user,
                                                          @Value("${app.rabbit.data.password}") String password) {
        return connection(host, port, user, password);
    }

    @Bean
    public CachingConnectionFactory syncConnectionFactory(@Value("${app.rabbit.sync.host}") String host,
                                                          @Value("${app.rabbit.sync.port}") int port,
                                                          @Value("${app.rabbit.sync.username}") String user,
                                                          @Value("${app.rabbit.sync.password}") String password) {
        return connection(host, port, user, password);
    }

    private static CachingConnectionFactory connection(String host, int port, String user, String password) {
        CachingConnectionFactory cf = new CachingConnectionFactory(host, port);
        cf.setUsername(user);
        cf.setPassword(password);
        return cf;
    }

    @Bean
    public RabbitAdmin dataAdmin(@Qualifier("dataConnectionFactory") ConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }

    @Bean
    public RabbitAdmin syncAdmin(@Qualifier("syncConnectionFactory") ConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }

    // ------------------------------------------------------------------ data collection broker
    @Bean
    public Queue measurementsQueue(@Value("${app.rabbit.data.queue}") String name,
                                   @Qualifier("dataAdmin") RabbitAdmin dataAdmin) {
        Queue queue = new Queue(name, true);          // durable: measurements survive a broker restart
        queue.setAdminsThatShouldDeclare(dataAdmin);
        return queue;
    }

    // ------------------------------------------------------------------ synchronization broker
    @Bean
    public TopicExchange syncExchange(@Value("${app.rabbit.sync.exchange}") String name,
                                      @Qualifier("syncAdmin") RabbitAdmin syncAdmin) {
        TopicExchange exchange = new TopicExchange(name, true, false);
        exchange.setAdminsThatShouldDeclare(syncAdmin);
        return exchange;
    }

    @Bean
    public Queue deviceSyncQueue(@Value("${app.rabbit.sync.queue}") String name,
                                 @Qualifier("syncAdmin") RabbitAdmin syncAdmin) {
        Queue queue = new Queue(name, true);
        queue.setAdminsThatShouldDeclare(syncAdmin);
        return queue;
    }

    @Bean
    public Binding deviceSyncBinding(@Qualifier("deviceSyncQueue") Queue queue,
                                     @Qualifier("syncExchange") TopicExchange exchange,
                                     @Qualifier("syncAdmin") RabbitAdmin syncAdmin) {
        Binding binding = BindingBuilder.bind(queue).to(exchange).with("device.*");
        binding.setAdminsThatShouldDeclare(syncAdmin);
        return binding;
    }

    // ------------------------------------------------------------------ listener containers
    @Bean(name = DATA_LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory dataListenerFactory(
            @Qualifier("dataConnectionFactory") ConnectionFactory cf) {
        return listenerFactory(cf);
    }

    @Bean(name = SYNC_LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory syncListenerFactory(
            @Qualifier("syncConnectionFactory") ConnectionFactory cf) {
        return listenerFactory(cf);
    }

    private static SimpleRabbitListenerContainerFactory listenerFactory(ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setConcurrentConsumers(1);            // one consumer thread: hourly totals are updated in order
        factory.setDefaultRequeueRejected(false);     // a poison message is dropped instead of looping forever
        return factory;
    }
}
