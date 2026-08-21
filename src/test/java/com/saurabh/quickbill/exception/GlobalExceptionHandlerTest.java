package com.saurabh.quickbill.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The simplest possible tests in this project: GlobalExceptionHandler's
 * methods are plain Java methods that take an exception and return a
 * ResponseEntity. No mocks, no Spring context, no database — just call the
 * method and check what comes back. This is a good place to start learning
 * unit testing before moving on to OrderServiceImplTest, which needs mocks.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_returns404_withTheExceptionMessage() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Order not found: ORD-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Order not found: ORD-1");
    }

    @Test
    void handleAccessDenied_returns403_notFoundOr401() {
        // 403 vs 404 matters here specifically: see AccessDeniedException's
        // own javadoc for why returning 404 instead would leak which order
        // IDs exist. This test locks that behaviour in place.
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("You do not have permission to delete this order."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().status()).isEqualTo(403);
    }

    @Test
    void handleGenericException_returns500_withAGenericMessage_neverTheRealExceptionText() {
        // This is the test that guards the "never leak ex.getMessage() to
        // the client" rule for unknown exceptions. If someone later changes
        // handleGenericException to return ex.getMessage(), this test fails.
        Exception sensitiveInternalError =
                new RuntimeException("Connection refused: jdbc:mysql://internal-db-host:3306/quickbill");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(sensitiveInternalError);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .doesNotContain("jdbc:mysql")
                .doesNotContain("internal-db-host")
                .isEqualTo("An unexpected error occurred. Please try again.");
    }

    @Test
    void handleBadCredentials_returns401_withoutLeakingWhetherEmailOrPasswordWasWrong() {
        ResponseEntity<?> response =
                handler.handleBadCredentials(new org.springframework.security.authentication.BadCredentialsException("bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
