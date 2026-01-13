package br.com.microservices.orchestrated.orderservice.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.start-saga}")
    private String startSagaTopic;

    public void sendEvent(String payload) {
        try {
            log.info("saga start event sent to topic: [{}] with data: [{}]", startSagaTopic, payload);
            kafkaTemplate.send(startSagaTopic, payload);
        } catch (Exception e) {
            log.error("error sending saga start event: [{}] with data: [{}]", startSagaTopic, payload, e);
        }
    }

}
