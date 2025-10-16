package me.agfe.wedsnap.exception;

import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.CommonApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return CommonApiResponse.error(ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, "잘못된 요청").build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public CommonApiResponse<Void> handleResponseStatus(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {}", ex.getMessage());
        return CommonApiResponse.error(ErrorResponse.builder(ex, ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : "요청 처리 중 오류 발생").build());
    }

    @ExceptionHandler(Exception.class)
    public CommonApiResponse<Void> handleGeneral(Exception ex) {
        log.warn("Exception: {}", ex.getMessage());
        return CommonApiResponse.error(ErrorResponse.builder(ex, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류").build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonApiResponse<Void> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
        return CommonApiResponse.error(ErrorResponse.builder(ex, HttpStatus.PAYLOAD_TOO_LARGE, "업로드 용량 초과").build());
    }
}
