package me.agfe.wedsnap.exception;

import lombok.Getter;

/**
 * WedSnap 애플리케이션의 커스텀 예외 클래스
 * ErrorCode와 함께 예외를 발생시켜 일관된 에러 응답을 제공합니다.
 */
@Getter
public class WedSnapException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    /**
     * ErrorCode를 사용하여 예외를 생성합니다.
     * @param errorCode 에러 코드
     */
    public WedSnapException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * ErrorCode와 상세 메시지를 사용하여 예외를 생성합니다.
     * @param errorCode 에러 코드
     * @param detail 상세 메시지
     */
    public WedSnapException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * ErrorCode와 원인 예외를 사용하여 예외를 생성합니다.
     * @param errorCode 에러 코드
     * @param cause 원인 예외
     */
    public WedSnapException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = cause.getMessage();
    }

    /**
     * ErrorCode, 상세 메시지, 원인 예외를 사용하여 예외를 생성합니다.
     * @param errorCode 에러 코드
     * @param detail 상세 메시지
     * @param cause 원인 예외
     */
    public WedSnapException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
