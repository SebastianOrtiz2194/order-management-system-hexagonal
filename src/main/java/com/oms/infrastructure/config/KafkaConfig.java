package com.oms.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuración central de Kafka: topics, error handling y Dead Letter Queue (DLQ).
 * <p>
 * Spring Boot auto-configura el {@code KafkaTemplate}, {@code ProducerFactory} y {@code ConsumerFactory}
 * a partir de las propiedades en {@code application.yml}. Aquí definimos únicamente los beans
 * que requieren personalización explícita: topics y la estrategia de manejo de errores.
 */
@Configuration
@Slf4j
public class KafkaConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    /**
     * Sufijo estándar de Spring Kafka para Dead Letter Topics.
     * Los mensajes que agotan sus reintentos se redirigen automáticamente a este topic.
     */
    public static final String ORDER_EVENTS_DLT = "order-events.DLT";

    /**
     * Número máximo de reintentos antes de enviar el mensaje al DLT.
     */
    private static final long MAX_RETRY_ATTEMPTS = 3;

    /**
     * Intervalo fijo entre reintentos (en milisegundos).
     */
    private static final long RETRY_INTERVAL_MS = 1_000L;

    // ===================================================================
    // TOPICS
    // ===================================================================

    /**
     * Topic principal para eventos de órdenes.
     * 3 particiones permiten paralelizar la carga entre múltiples consumers del mismo grupo.
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)  // En entorno local solo tenemos 1 Broker
                .build();
    }

    /**
     * Dead Letter Topic (DLT) para mensajes que fallaron tras agotar los reintentos.
     * <p>
     * Usar una sola partición es suficiente para el DLT ya que el volumen
     * de mensajes fallidos debería ser bajo. En producción, ajustar según el throughput esperado.
     */
    @Bean
    public NewTopic orderEventsDltTopic() {
        return TopicBuilder.name(ORDER_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ===================================================================
    // ERROR HANDLING & DLQ
    // ===================================================================

    /**
     * Configura el {@link DeadLetterPublishingRecoverer} que redirige los mensajes fallidos
     * al Dead Letter Topic después de agotar los reintentos del {@link DefaultErrorHandler}.
     * <p>
     * El recoverer preserva los headers originales del mensaje (incluido el stack trace del error),
     * facilitando el diagnóstico y el reprocesamiento manual desde el DLT.
     *
     * @param kafkaOperations el {@code KafkaTemplate} que se usará para publicar al DLT
     * @return el recoverer configurado
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaOperations<Object, Object> kafkaOperations) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                // Estrategia de routing: todos los mensajes fallidos van al DLT fijo
                (ConsumerRecord<?, ?> record, Exception ex) -> new org.apache.kafka.common.TopicPartition(
                        ORDER_EVENTS_DLT, -1  // -1 = dejar a Kafka elegir la partición
                ));

        log.info("DeadLetterPublishingRecoverer configured — failed messages route to [{}]", ORDER_EVENTS_DLT);
        return recoverer;
    }

    /**
     * Configura el {@link DefaultErrorHandler} como la estrategia global de manejo de errores
     * para todos los {@code @KafkaListener} de la aplicación.
     * <p>
     * Comportamiento:
     * <ol>
     *   <li>Ante un error en el consumer, reintenta hasta {@value MAX_RETRY_ATTEMPTS} veces
     *       con un intervalo fijo de {@value RETRY_INTERVAL_MS}ms entre cada reintento.</li>
     *   <li>Si todos los reintentos fallan, el {@link DeadLetterPublishingRecoverer} envía
     *       el mensaje al topic {@code order-events.DLT} para inspección o reprocesamiento manual.</li>
     * </ol>
     *
     * @param recoverer el recoverer que redirige al DLT
     * @return el error handler configurado
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // FixedBackOff(intervalMs, maxAttempts): reintenta MAX_RETRY_ATTEMPTS veces con RETRY_INTERVAL_MS de espera
        FixedBackOff backOff = new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Log cada reintento para visibilidad operacional
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("|| KAFKA RETRY || Attempt {}/{} for topic [{}], partition [{}], offset [{}]. Error: {}",
                        deliveryAttempt, MAX_RETRY_ATTEMPTS,
                        record.topic(), record.partition(), record.offset(),
                        ex.getMessage())
        );

        log.info("DefaultErrorHandler configured — maxRetries={}, intervalMs={}, DLT=[{}]",
                MAX_RETRY_ATTEMPTS, RETRY_INTERVAL_MS, ORDER_EVENTS_DLT);

        return errorHandler;
    }
}
