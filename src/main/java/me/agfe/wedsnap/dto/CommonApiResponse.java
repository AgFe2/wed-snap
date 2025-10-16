package me.agfe.wedsnap.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.ErrorResponse;

@Getter
@Builder
public class CommonApiResponse<T> {
    private boolean result;
    private T data;
    private ErrorResponse error;

    public static<T> CommonApiResponse<T> error(ErrorResponse error) {
        return CommonApiResponse.<T>builder()
                .result(false)
                .error(error)
                .build();
    }

    public static<T> CommonApiResponse<T> success(T data){
        return CommonApiResponse.<T>builder()
                .result(true)
                .data(data)
                .build();
    }
}
