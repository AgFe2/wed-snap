package me.agfe.wedsnap.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.HealthCheckResponse;

/**
 * Health Check API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Health Check", description = "서버 상태 확인 API")
public class HealthCheckController {

    @Operation(
            summary = "서버 헬스 체크",
            description = "서버가 정상적으로 동작하는지 확인합니다. 서버의 현재 상태와 타임스탬프를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "서버 정상 동작 중",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = HealthCheckResponse.class)
                            )
                    )
            })
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
