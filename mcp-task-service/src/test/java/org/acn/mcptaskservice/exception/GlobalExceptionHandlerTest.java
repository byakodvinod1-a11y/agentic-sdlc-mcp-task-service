package org.acn.mcptaskservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handle_shouldReturnBadRequestForIllegalArgumentException() {
        ResponseEntity<?> response = handler.handle(
                new IllegalArgumentException("Invalid dueDate format. Expected YYYY-MM-DD.")
        );

        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertNotNull(body);
        assertEquals("Invalid dueDate format. Expected YYYY-MM-DD.", body.get("error"));
    }

    @Test
    void handle_shouldReturnBadRequestForGenericException() {
        ResponseEntity<?> response = handler.handle(
                new RuntimeException("Unknown tool: bad-tool")
        );

        assertEquals(400, response.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertNotNull(body);
        assertEquals("Unknown tool: bad-tool", body.get("error"));
    }
}