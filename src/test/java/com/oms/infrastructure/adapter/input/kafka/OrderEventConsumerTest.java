package com.oms.infrastructure.adapter.input.kafka;

import com.oms.application.port.output.OrderCachePort;
import com.oms.domain.event.OrderCreatedEvent;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderEventConsumer}.
 * <p>
 * Validates the consumer's resilience behaviors:
 * <ul>
 *   <li>Happy path: event is processed and cached correctly.</li>
 *   <li>Null payload: skipped without exception (no retry needed).</li>
 *   <li>Null required fields: skipped without exception.</li>
 *   <li>Invalid status: caught as non-retryable, no re-throw.</li>
 *   <li>Unexpected errors (e.g. Redis failure): re-thrown for DefaultErrorHandler retry + DLQ.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer — Kafka Resilience Tests")
class OrderEventConsumerTest {

    @Mock
    private OrderCachePort cachePort;

    @InjectMocks
    private OrderEventConsumer consumer;

    // Common test fixtures
    private static final String TOPIC = "order-events";
    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    private OrderCreatedEvent validEvent() {
        return new OrderCreatedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Alice Johnson",
                new BigDecimal("300.00"),
                "PENDING"
        );
    }

    // =======================================================================
    // HAPPY PATH
    // =======================================================================

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should process valid event and save to cache")
        void shouldProcessValidEventAndSaveToCache() {
            // given
            OrderCreatedEvent event = validEvent();

            // when
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // then
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(cachePort, times(1)).save(orderCaptor.capture());

            Order cached = orderCaptor.getValue();
            assertThat(cached.getId()).isEqualTo(event.orderId());
            assertThat(cached.getCustomerName()).isEqualTo(event.customerName());
            assertThat(cached.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(cached.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should process event with CONFIRMED status correctly")
        void shouldProcessEventWithConfirmedStatus() {
            // given
            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID(), "Bob Smith", new BigDecimal("150.00"), "CONFIRMED");

            // when
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // then
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(cachePort).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    // =======================================================================
    // NULL / INVALID PAYLOAD — Non-retryable
    // =======================================================================

    @Nested
    @DisplayName("Null & Invalid Payload Handling (Non-Retryable)")
    class NullPayloadHandling {

        @Test
        @DisplayName("Should skip processing when event payload is null")
        void shouldSkipWhenEventIsNull() {
            // when
            consumer.consumeOrderCreated(null, TOPIC, PARTITION, OFFSET);

            // then — no interaction with cache, no exception thrown
            verify(cachePort, never()).save(any());
        }

        @Test
        @DisplayName("Should skip processing when orderId is null")
        void shouldSkipWhenOrderIdIsNull() {
            // given
            OrderCreatedEvent event = new OrderCreatedEvent(
                    null, "Alice", new BigDecimal("100.00"), "PENDING");

            // when
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // then
            verify(cachePort, never()).save(any());
        }

        @Test
        @DisplayName("Should skip processing when customerName is null")
        void shouldSkipWhenCustomerNameIsNull() {
            // given
            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID(), null, new BigDecimal("100.00"), "PENDING");

            // when
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // then
            verify(cachePort, never()).save(any());
        }

        @Test
        @DisplayName("Should skip processing when status is null")
        void shouldSkipWhenStatusIsNull() {
            // given
            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID(), "Alice", new BigDecimal("100.00"), null);

            // when
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // then
            verify(cachePort, never()).save(any());
        }

        @Test
        @DisplayName("Should not throw on invalid status — non-retryable error is caught internally")
        void shouldNotThrowOnInvalidStatus() {
            // given — "INVALID_STATUS" is not a valid OrderStatus enum value
            OrderCreatedEvent event = new OrderCreatedEvent(
                    UUID.randomUUID(), "Alice", new BigDecimal("100.00"), "INVALID_STATUS");

            // when/then — should not throw (caught internally as non-retryable)
            consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET);

            // Cache should never be written on a failed parse
            verify(cachePort, never()).save(any());
        }
    }

    // =======================================================================
    // RETRYABLE ERRORS — Re-thrown for DefaultErrorHandler
    // =======================================================================

    @Nested
    @DisplayName("Retryable Errors (Re-thrown for retry + DLQ)")
    class RetryableErrors {

        @Test
        @DisplayName("Should re-throw RuntimeException from cache for DefaultErrorHandler to retry")
        void shouldRethrowRuntimeExceptionFromCache() {
            // given
            OrderCreatedEvent event = validEvent();
            doThrow(new RuntimeException("Redis connection timeout"))
                    .when(cachePort).save(any(Order.class));

            // when/then — the exception MUST propagate so DefaultErrorHandler can apply retry policy
            assertThatThrownBy(() ->
                    consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis connection timeout");
        }

        @Test
        @DisplayName("Should re-throw any unexpected exception from cache save")
        void shouldRethrowUnexpectedExceptionFromCacheSave() {
            // given
            OrderCreatedEvent event = validEvent();
            doThrow(new RuntimeException("Serialization failure"))
                    .when(cachePort).save(any(Order.class));

            // when/then
            assertThatThrownBy(() ->
                    consumer.consumeOrderCreated(event, TOPIC, PARTITION, OFFSET))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Serialization failure");

            verify(cachePort, times(1)).save(any());
        }
    }
}
