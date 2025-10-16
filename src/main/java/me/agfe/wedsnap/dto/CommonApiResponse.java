package me.agfe.wedsnap.dto;

import org.springframework.web.ErrorResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommonApiResponse<T> {

    public static <T> CommonApiResponse<T> error(ErrorResponse error) {
        return CommonApiResponse.<T>builder()
                                .result(false)
                                .error(error)
                                .build();
    }

    public static <T> CommonApiResponse<T> success(T data) {
        return CommonApiResponse.<T>builder()
                                .result(true)
                                .data(data)
                                .build();
    }
    private boolean result;
    private T data;
    private ErrorResponse error;
}
