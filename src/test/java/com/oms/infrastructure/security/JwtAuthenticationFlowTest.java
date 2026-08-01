package com.oms.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.application.port.input.CreateOrderUseCase;
import com.oms.application.port.input.GetOrderUseCase;
import com.oms.application.port.input.UpdateOrderStatusUseCase;
import com.oms.domain.model.Order;
import com.oms.domain.model.PagedResult;
import com.oms.infrastructure.adapter.input.rest.AuthController;
import com.oms.infrastructure.adapter.input.rest.OrderController;
import com.oms.infrastructure.adapter.input.rest.OrderRestMapper;
import com.oms.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice Test — JWT Security Flow (Web layer only, no infrastructure required).
 *
 * <p>Exercises the complete authentication lifecycle against the real security filter chain:
 * credential verification at the token endpoint, JWT signing, and stateless Bearer-token
 * validation on protected endpoints. Unlike the Testcontainers-based ITs, this test boots
 * only the web slice and therefore runs anywhere, including CI without Docker.</p>
 */
@WebMvcTest(
        controllers = {AuthController.class, OrderController.class},
        properties = {
                "oms.security.username=admin",
                "oms.security.password=admin123",
                "oms.security.jwt.issuer=oms-test",
                "oms.security.jwt.secret=test-secret-key-with-at-least-32-bytes!!",
                "oms.security.jwt.expiration-minutes=30"
        }
)
@Import({SecurityConfig.class, JwtTokenService.class})
class JwtAuthenticationFlowTest {

    private static final String TOKEN_URL = "/api/v1/auth/token";
    private static final String ORDERS_URL = "/api/v1/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private GetOrderUseCase getOrderUseCase;

    @MockBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @MockBean
    private OrderRestMapper orderRestMapper;

    // ─── POST /api/v1/auth/token ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/token - 200 OK issues a signed JWT for valid credentials")
    void issueToken_withValidCredentials_returnsJwt() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(1800));
    }

    @Test
    @DisplayName("POST /auth/token - 401 Unauthorized for wrong password")
    void issueToken_withInvalidPassword_returns401() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /auth/token - 401 Unauthorized for unknown user")
    void issueToken_withUnknownUser_returns401() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /auth/token - 400 Bad Request when credentials are blank")
    void issueToken_withBlankCredentials_returns400() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── Bearer Token Validation on Protected Endpoints ──────────────────────────

    @Test
    @DisplayName("GET /orders - 200 OK when presenting a freshly issued JWT (full E2E flow)")
    void protectedEndpoint_withIssuedToken_returns200() throws Exception {
        // Given: a token obtained from the real issuance endpoint.
        String token = obtainAccessToken("admin", "admin123");
        when(getOrderUseCase.getAllOrders(anyInt(), anyInt(), isNull()))
                .thenReturn(new PagedResult<Order>(List.of(), 0, 20, 0L, 0));

        // When / Then: the resource server validates the Bearer token and serves the request.
        mockMvc.perform(get(ORDERS_URL)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /orders - 401 Unauthorized when no token is presented")
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(ORDERS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /orders - 401 Unauthorized for a tampered/garbage token")
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get(ORDERS_URL)
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /orders - 401 Unauthorized for a token signed with a different secret")
    void protectedEndpoint_withForeignSignature_returns401() throws Exception {
        // Given: a structurally valid JWT signed with an unknown key.
        String foreignToken = obtainAccessToken("admin", "admin123")
                .replaceFirst("^[^.]+", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");

        mockMvc.perform(get(ORDERS_URL)
                        .header("Authorization", "Bearer " + foreignToken + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String obtainAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("access_token").asText();
    }
}
