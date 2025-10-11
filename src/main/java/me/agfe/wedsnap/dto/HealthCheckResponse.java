package me.agfe.wedsnap.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Health Check API 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "헬스 체크 응답")
public class HealthCheckResponse {

    @Schema(description = "서버 상태", example = "UP")
    private String status;

    @Schema(description = "상태 메시지", example = "서버가 정상적으로 동작 중입니다.")
    private String message;

    @Schema(description = "응답 시간", example = "2025-10-12T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "서버 정보", example = "WedSnap Server v1.0")
    private String serverInfo;
}
