package org.acn.mcptaskservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_shouldReturnBadRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException("Invalid dueDate format. Expected YYYY-MM-DD.")
                );

        assertEquals(400, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid dueDate format. Expected YYYY-MM-DD.", body.get("error"));
    }

    @Test
    void handleDataAccess_shouldReturnInternalServerError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDataAccess(
                        new DataAccessResourceFailureException("Database unavailable")
                );

        assertEquals(500, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Database error", body.get("error"));
    }

    @Test
    void handleGeneric_shouldReturnInternalServerError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(
                        new RuntimeException("Unknown tool: bad-tool")
                );

        assertEquals(500, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Internal server error", body.get("error"));
    }
}