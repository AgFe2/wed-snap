package me.agfe.wedsnap.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_VALUE("INVALID_VALUE", "잘못된 요청", "입력값이 올바르지 않습니다."),
    FILE_SIZE_EXCEEDED("FILE_SIZE_EXCEEDED", "업로드 실패", "파일 용량이 초과되었습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류", "서버 처리 중 오류가 발생했습니다."),
    CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "유효성 검사 실패", "입력값 검증에 실패했습니다."),
    RESPONSE_STATUS_ERROR("RESPONSE_STATUS_ERROR", "HTTP 상태 오류", "요청 처리 중 오류 발생"),
    MISSING_REQUEST_PARAMETER("MISSING_REQUEST_PARAMETER", "필수 요청 파라미터 누락", "필수 요청 파라미터가 존재하지 않습니다."),
    NOT_MULTIPART_REQUEST("NOT_MULTIPART_REQUEST", "multipart 요청 아님", "요청이 multipart/form-data 형식이 아닙니다.");

    private final String code;
    private final String title;
    private final String message;
}