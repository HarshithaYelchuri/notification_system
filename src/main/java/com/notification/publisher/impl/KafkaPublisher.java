package com.notification.publisher.impl;

import com.notification.publisher.GenericPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher implements GenericPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(String topic, String key, Object payload, Map<String, String> headers) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach((hKey, hValue) -> {
                if (hValue != null) {
                    record.headers().add(new RecordHeader(hKey, hValue.getBytes(StandardCharsets.UTF_8)));
                }
            });
        }

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish message to topic [{}], key [{}]: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Successfully published message to topic [{}], partition [{}], offset [{}]",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
