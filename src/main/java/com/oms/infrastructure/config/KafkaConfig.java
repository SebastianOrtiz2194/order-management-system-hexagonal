package com.oms.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Spring Boot se encarga de crear el topic en el momento de arrancar
 * si su 'KAFKA_AUTO_CREATE_TOPICS_ENABLE' no está inyectado como 'false' en docker-compose
 */
@Configuration
public class KafkaConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(3)         // Ideal para paralelizar cargas de trabajo masivas
                .replicas(1)           // En entorno local solo tenemos 1 Broker
                .build();
    }
}
