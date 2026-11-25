package ro.tuc.ds.monitoring.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds.monitoring.config.Json;
import ro.tuc.ds.monitoring.config.RabbitConfig;
import ro.tuc.ds.monitoring.dto.MeasurementMessage;
import ro.tuc.ds.monitoring.service.MeasurementService;

import java.nio.charset.StandardCharsets;

/** Consumes the smart meter readings sent by the Device Data Simulator to the Data Collection Broker. */
@Component
public class MeasurementConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementConsumer.class);

    private final MeasurementService service;

    public MeasurementConsumer(MeasurementService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbit.data.queue}", containerFactory = RabbitConfig.DATA_LISTENER_FACTORY)
    public void onMeasurement(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        // Assignment requirement: display the messages extracted from the queue (console)
        LOGGER.info("[data-broker] message extracted from queue: {}", body);
        try {
            service.process(Json.read(body, MeasurementMessage.class));
        } catch (RuntimeException e) {
            LOGGER.warn("[data-broker] rejected invalid message ({}): {}", e.getMessage(), body);
            throw new AmqpRejectAndDontRequeueException("Invalid measurement: " + e.getMessage(), e);
        }
    }
}
