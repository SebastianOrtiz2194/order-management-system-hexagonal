package com.oms.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.application.port.output.OrderCachePort;
import com.oms.application.port.output.OrderEventPublisherPort;
import com.oms.infrastructure.adapter.input.rest.OrderDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test — REST Layer (End-to-End API).
 *
 * <p>Validates the full application lifecycle from HTTP requests through to the relational database. 
 * Testcontainers manages a dedicated PostgreSQL instance. Redis and Kafka are isolated via {@code @MockBean} 
 * to maintain focus on the REST-to-Domain-to-PostgreSQL flow.</p>
 *
 * <p>{@code @AutoConfigureMockMvc} provides a mock HTTP client that simulates requests without 
 * binding to a real network port.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class OrderControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("oms_db_it")
            .withUsername("it_user")
            .withPassword("it_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /** Mocked cache port to simulate cache misses during retrieval. */
    @MockBean
    private OrderCachePort orderCachePort;

    /** Mocked Kafka publisher to avoid broker interaction overhead. */
    @MockBean
    private OrderEventPublisherPort orderEventPublisherPort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/orders";

    // ─── Factory Helpers ─────────────────────────────────────────────────────────

    private OrderDTOs.CreateOrderRequest buildValidCreateRequest() {
        OrderDTOs.OrderItemRequest item = new OrderDTOs.OrderItemRequest(
                "PROD-TEST-001",
                "Mechanical Keyboard",
                1,
                new BigDecimal("199.99")
        );
        return new OrderDTOs.CreateOrderRequest("John Smith", List.of(item));
    }

    // ─── POST /api/v1/orders ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /orders - 201 Created with valid payload")
    void createOrder_withValidPayload_returns201() throws Exception {
        String requestJson = objectMapper.writeValueAsString(buildValidCreateRequest());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerName").value("John Smith"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(199.99))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productId").value("PROD-TEST-001"));
    }

    @Test
    @DisplayName("POST /orders - 400 Bad Request when customerName is blank")
    void createOrder_withBlankCustomerName_returns400() throws Exception {
        OrderDTOs.OrderItemRequest item = new OrderDTOs.OrderItemRequest(
                "PROD-001", "Keyboard", 1, new BigDecimal("99.00"));
        OrderDTOs.CreateOrderRequest badRequest = new OrderDTOs.CreateOrderRequest("", List.of(item));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /orders - 400 Bad Request when items list is empty")
    void createOrder_withEmptyItems_returns400() throws Exception {
        OrderDTOs.CreateOrderRequest badRequest = new OrderDTOs.CreateOrderRequest("John", List.of());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/v1/orders/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /orders/{id} - 200 OK returns existing order")
    void getOrderById_whenOrderExists_returns200() throws Exception {
        // Given: Create an initial order to populate the database.
        String createJson = objectMapper.writeValueAsString(buildValidCreateRequest());
        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        OrderDTOs.OrderResponse created = objectMapper.readValue(responseBody, OrderDTOs.OrderResponse.class);

        // When / Then: Verify retrieval by ID.
        mockMvc.perform(get(BASE_URL + "/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.customerName").value("John Smith"));
    }

    @Test
    @DisplayName("GET /orders/{id} - 404 Not Found when order does not exist")
    void getOrderById_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─── GET /api/v1/orders ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /orders - 200 OK returns paginated list of orders")
    void getAllOrders_returns200WithList() throws Exception {
        // Given: Seed the database with a test order.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest())))
                .andExpect(status().isCreated());

        // When / Then: Verify paginated retrieval.
        mockMvc.perform(get(BASE_URL).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.currentPage").isNumber());
    }

    // ─── PATCH /api/v1/orders/{id}/status ────────────────────────────────────────

    @Test
    @DisplayName("PATCH /orders/{id}/status - 200 OK updates order status")
    void updateStatus_withValidTransition_returns200() throws Exception {
        // Given: Seed the database with a test order.
        String createJson = objectMapper.writeValueAsString(buildValidCreateRequest());
        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andReturn();

        OrderDTOs.OrderResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderDTOs.OrderResponse.class);

        OrderDTOs.UpdateStatusRequest updateRequest = new OrderDTOs.UpdateStatusRequest("CONFIRMED");

        // When / Then: Verify status update.
        mockMvc.perform(patch(BASE_URL + "/" + created.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status - 404 Not Found for non-existent order")
    void updateStatus_whenOrderNotFound_returns404() throws Exception {
        OrderDTOs.UpdateStatusRequest updateRequest = new OrderDTOs.UpdateStatusRequest("CONFIRMED");

        mockMvc.perform(patch(BASE_URL + "/00000000-0000-0000-0000-000000000000/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /orders/{id}/status - 400 Bad Request for invalid status value")
    void updateStatus_withInvalidStatus_returns400() throws Exception {
        // Given: Seed the database.
        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidCreateRequest())))
                .andReturn();
        OrderDTOs.OrderResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderDTOs.OrderResponse.class);

        OrderDTOs.UpdateStatusRequest badRequest = new OrderDTOs.UpdateStatusRequest("INVALID_STATUS");

        // When / Then: Verify that invalid enum values trigger a 400 Bad Request.
        mockMvc.perform(patch(BASE_URL + "/" + created.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    // ─── SECURITY VERIFICATION ───────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("ANY /orders - 401 Unauthorized when no user is provided")
    void anyEndpoint_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Swagger - 200 OK without authentication")
    void swaggerEndpoints_withoutAuthentication_arePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound()); // Expect redirect to swagger-ui/index.html
    }
}
