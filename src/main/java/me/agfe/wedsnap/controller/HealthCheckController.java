package me.agfe.wedsnap.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.HealthCheckResponse;

/**
 * Health Check API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        log.info("Health check requested");

        HealthCheckResponse response = HealthCheckResponse.builder()
                                                          .status("UP")
                                                          .message("서버가 정상적으로 동작 중입니다.")
                                                          .timestamp(LocalDateTime.now())
                                                          .serverInfo("WedSnap Server v1.0")
                                                          .build();

        return ResponseEntity.ok(response);
    }
}
