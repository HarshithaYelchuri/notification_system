package com.notification.publisher;

import java.util.Map;

public interface GenericPublisher {

    void publish(String topic, String key, Object payload, Map<String, String> headers);

    default void publish(String topic, String key, Object payload) {
        publish(topic, key, payload, Map.of());
    }
}
