package me.agfe.wedsnap.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_VALUE("INVALID_VALUE", "잘못된 요청", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED", "업로드 실패", "파일 용량이 초과되었습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류", "서버 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "유효성 검사 실패", "입력값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    RESPONSE_STATUS_ERROR("RESPONSE_STATUS_ERROR", "HTTP 상태 오류", "요청 처리 중 오류 발생", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String title;
    private final String message;
    private final HttpStatus httpStatus;
}