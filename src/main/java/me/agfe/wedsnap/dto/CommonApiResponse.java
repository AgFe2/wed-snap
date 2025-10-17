package me.agfe.wedsnap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
