package me.agfe.wedsnap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import me.agfe.wedsnap.dto.CommonApiResponse;
import me.agfe.wedsnap.dto.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_VALUE;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonApiResponse<Void> handleResponseStatus(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.RESPONSE_STATUS_ERROR;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.CONSTRAINT_VIOLATION;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonApiResponse<Void> handleGeneral(Exception ex) {
        log.warn("Exception: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public CommonApiResponse<Void> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonApiResponse<Void> handleMissingParameterException(MissingServletRequestParameterException ex) {
        log.warn("MissingServletRequestParameterException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonApiResponse<Void> handleMultipartException(MultipartException ex) {
        log.warn("MultipartException: {}", ex.getMessage());
        ErrorCode errorCode = ErrorCode.NOT_MULTIPART_REQUEST;
        return CommonApiResponse.error(
                ErrorResponse.builder()
                             .errorCode(errorCode.getCode())
                             .title(errorCode.getTitle())
                             .message(errorCode.getMessage())
                             .detail(ex.getMessage())
                             .build()
        );
    }
}
