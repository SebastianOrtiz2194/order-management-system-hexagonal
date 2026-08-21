package com.oms.infrastructure.adapter.input.rest;

import com.oms.domain.exception.InvalidOrderException;
import com.oms.domain.exception.OrderNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * <p>
 * Validates that every supported exception type is mapped to the correct HTTP status
 * and that the generic fallback never leaks internal implementation details.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Map<String, Object> bodyOf(ResponseEntity<Map<String, Object>> response) {
        return response.getBody();
    }

    // Dummy method used only to build a MethodParameter for handler-validation tests.
    @SuppressWarnings("unused")
    private void dummyPagination(@Min(0) int page, @Min(1) @Max(100) int size) {
    }

    private record PaginationParams(@Min(0) int page, @Min(1) @Max(100) int size) {
    }

    @Nested
    @DisplayName("Domain exceptions")
    class DomainExceptions {

        @Test
        @DisplayName("OrderNotFoundException maps to 404 with its message")
        void orderNotFound_mapsTo404() {
            ResponseEntity<Map<String, Object>> response =
                    handler.handleNotFound(new OrderNotFoundException("Order with ID abc not found."));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response)).containsEntry("status", 404);
            assertThat(bodyOf(response)).containsEntry("message", "Order with ID abc not found.");
        }

        @Test
        @DisplayName("InvalidOrderException maps to 400 with its message")
        void invalidOrder_mapsTo400() {
            ResponseEntity<Map<String, Object>> response =
                    handler.handleInvalidOrder(new InvalidOrderException("Invalid status transition from PENDING to SHIPPED"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response)).containsEntry("message", "Invalid status transition from PENDING to SHIPPED");
        }
    }

    @Nested
    @DisplayName("Request binding failures")
    class RequestBindingFailures {

        @Test
        @DisplayName("MethodArgumentNotValidException maps to 400 with field details")
        void beanValidationErrors_mapTo400() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                    new OrderDTOs.CreateOrderRequest("Bob", java.util.List.of()), "request");
            bindingResult.rejectValue("customerName", "NotBlank", "Customer name is mandatory");

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<Map<String, Object>> response = handler.handleValidations(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat((String) bodyOf(response).get("message"))
                    .contains("Validations failed.")
                    .contains("customerName: Customer name is mandatory");
        }

        @Test
        @DisplayName("Malformed JSON maps to 400 without exposing parse internals")
        void malformedJson_mapsTo400() {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("JSON parse error: Unexpected character ('x')");

            ResponseEntity<Map<String, Object>> response = handler.handleUnreadableBody(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response)).containsEntry("message", "Malformed JSON request body");
        }

        @Test
        @DisplayName("Unconvertible path variable (e.g., invalid UUID) maps to 400")
        void typeMismatch_mapsTo400() {
            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException("not-a-uuid", UUID.class, "id", null, null);

            ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat((String) bodyOf(response).get("message")).contains("Invalid value 'not-a-uuid' for parameter 'id'");
        }

        @Test
        @DisplayName("Method parameter violations (e.g., @Min/@Max on pagination) map to 400")
        void methodParameterViolations_mapTo400() throws Exception {
            MethodParameter param = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyPagination", int.class, int.class), 0);

            ParameterValidationResult paramResult = new ParameterValidationResult(
                    param, -1, List.of(new DefaultMessageSourceResolvable("Page must be zero or greater")));

            MethodValidationResult result = MethodValidationResult.create(new Object(), param.getMethod(), List.of(paramResult));
            HandlerMethodValidationException ex = new HandlerMethodValidationException(result);

            ResponseEntity<Map<String, Object>> response = handler.handleMethodValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat((String) bodyOf(response).get("message")).contains("Page must be zero or greater");
        }

        @Test
        @DisplayName("Bean Validation constraint violations map to 400 with the offending property")
        void constraintViolations_mapTo400() {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
            Set<ConstraintViolation<PaginationParams>> violations = validator.validate(new PaginationParams(0, 200));
            ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

            ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat((String) bodyOf(response).get("message"))
                    .contains("Validations failed.")
                    .contains("size");
        }
    }

    @Nested
    @DisplayName("Authentication and generic failures")
    class AuthenticationAndGenericFailures {

        @Test
        @DisplayName("BadCredentialsException maps to 401 with a static message")
        void badCredentials_mapsTo401() {
            ResponseEntity<Map<String, Object>> response =
                    handler.handleBadCredentials(new BadCredentialsException("Wrong password"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(bodyOf(response)).containsEntry("message", "Invalid username or password");
        }

        @Test
        @DisplayName("Unexpected exception maps to 500 and never leaks the original message")
        void unexpectedException_mapsTo500_withoutLeakingInternals() {
            String internalSecret = "jdbc:postgresql://10.0.0.5/oms_db password=supersecret";
            ResponseEntity<Map<String, Object>> response =
                    handler.handleAll(new IllegalStateException(internalSecret));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            String message = (String) bodyOf(response).get("message");
            assertThat(message).isEqualTo("An unexpected error occurred. Please try again later.");
            assertThat(message).doesNotContain("supersecret");
            assertThat(message).doesNotContain("jdbc");
        }
    }
}
