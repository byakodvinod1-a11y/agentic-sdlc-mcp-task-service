package org.acn.mcptaskservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "error", ex.getMessage()
                )
        );
    }
}