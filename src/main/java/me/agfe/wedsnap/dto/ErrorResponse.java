package me.agfe.wedsnap.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private String errorCode;
    private String title;
    private String message;
    private String detail;
}