package com.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.ingest:notification.ingest}")
    private String ingestTopic;

    @Value("${app.kafka.topics.enriched:notification.enriched}")
    private String enrichedTopic;

    @Value("${app.kafka.topics.audit:notification.audit}")
    private String auditTopic;

    @Value("${app.kafka.topics.retry:notification.retry}")
    private String retryTopic;

    @Value("${app.kafka.topics.dlq:notification.dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic ingestTopic() {
        return TopicBuilder.name(ingestTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic enrichedTopic() {
        return TopicBuilder.name(enrichedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(auditTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name(retryTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic).partitions(3).replicas(1).build();
    }
}
