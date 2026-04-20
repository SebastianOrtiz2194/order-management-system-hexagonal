package com.oms.infrastructure.persistence;

import com.oms.application.port.output.OrderCachePort;
import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.domain.model.Order;
import com.oms.domain.model.OrderItem;
import com.oms.domain.model.OrderStatus;
import com.oms.infrastructure.adapter.output.persistence.PostgresOrderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de Integración — Capa de Persistencia.
 *
 * <p>Usamos Testcontainers para levantar un contenedor real de PostgreSQL en Docker.
 * Redis y Kafka son reemplazados con {@code @MockBean} para aislar el test de
 * dependencias externas que no son relevantes para verificar la persistencia.</p>
 *
 * <p>@SpringBootTest levanta el contexto completo de Spring incluyendo Flyway,
 * JPA e Hibernate, asegurándonos de que el esquema real (V1__Initial_schema.sql)
 * es aplicado contra el contenedor antes de cada ejecución de test.</p>
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresOrderAdapterIT {

    /**
     * Declaramos el contenedor como @Container static para que Testcontainers lo
     * comparta entre todos los métodos de test. Esto evita levantar/bajar Docker
     * en cada @Test, lo que mejora drásticamente el tiempo de ejecución.
     */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("oms_db_test")
            .withUsername("test_user")
            .withPassword("test_password");

    /**
     * @DynamicPropertySource: el puente entre Testcontainers y Spring.
     * Sobreescribe dinámicamente las propiedades de datasource con las coordenadas
     * del contenedor que Docker eligió (puerto efímero, host, etc.).
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /**
     * @MockBean reemplaza el bean real de Redis por un Mock de Mockito en el
     * ApplicationContext. El adaptador de cache devuelve vacío por defecto,
     * lo que hace que el GetOrderService vaya directamente a la base de datos.
     */
    @MockBean
    private OrderCachePort orderCachePort;

    /**
     * @MockBean para Kafka. El publicador de eventos no hace nada durante los tests,
     * evitando timeouts de conexión al broker.
     */
    @MockBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @Autowired
    private PostgresOrderAdapter postgresOrderAdapter;

    // ─── Factory methods ────────────────────────────────────────────────────────

    /**
     * Creamos un Order de dominio de muestra para cada test, reutilizando la lógica
     * real del Aggregate Root (validateAndInitialize) en lugar de construir objetos
     * "mágicos". Esto prueba el dominio y la persistencia al mismo tiempo.
     */
    private Order buildSampleOrder() {
        OrderItem item = OrderItem.builder()
                .productId("PROD-001")
                .productName("Laptop Pro")
                .quantity(2)
                .unitPrice(new BigDecimal("1299.99"))
                .build();

        Order order = Order.builder()
                .customerName("Jane Doe")
                .items(List.of(item))
                .build();

        order.validateAndInitialize();
        return order;
    }

    // ─── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() persists an Order and its items correctly in PostgreSQL")
    void save_shouldPersistOrderWithItems() {
        // Given
        Order order = buildSampleOrder();

        // When
        Order saved = postgresOrderAdapter.save(order);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerName()).isEqualTo("Jane Doe");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(new BigDecimal("2599.98"));
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("findById() returns the order when it exists in the database")
    void findById_whenOrderExists_shouldReturnOrder() {
        // Given
        Order saved = postgresOrderAdapter.save(buildSampleOrder());

        // When
        Optional<Order> found = postgresOrderAdapter.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCustomerName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("findById() returns empty Optional when order does not exist")
    void findById_whenOrderNotExists_shouldReturnEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Order> result = postgresOrderAdapter.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAll() returns all persisted orders paginated")
    void findAll_shouldReturnAllPersistedOrders() {
        // Given
        postgresOrderAdapter.save(buildSampleOrder());
        postgresOrderAdapter.save(buildSampleOrder());

        // When
        var result = postgresOrderAdapter.findAll(0, 20, null);

        // Then
        // findAll() retorna la pagina con al menos los dos que acabamos de persistir
        assertThat(result.content()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("save() updates the status of an existing order")
    void save_shouldUpdateOrderStatus() {
        // Given
        Order saved = postgresOrderAdapter.save(buildSampleOrder());
        saved.updateStatus(OrderStatus.CONFIRMED);

        // When
        Order updated = postgresOrderAdapter.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
