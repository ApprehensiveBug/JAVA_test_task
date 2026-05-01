package com.pingine.fleetpulse.api;

import com.pingine.fleetpulse.service.TripNotFoundException;
import com.pingine.fleetpulse.service.VehicleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            VehicleNotFoundException.class,
            TripNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
