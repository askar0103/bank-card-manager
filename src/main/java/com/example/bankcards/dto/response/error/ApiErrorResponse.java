package com.example.bankcards.dto.response.error;

import com.example.bankcards.enums.ErrorCode;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        ErrorCode errorCode,
        String message,
        List<String> details,
        Instant timestamp,
        String path
) {
    public ApiErrorResponse(ErrorCode errorCode, String message, List<String> details, String path) {
        this(errorCode, message, details, Instant.now(), path);
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<String> details, String path) {
        return new ApiErrorResponse(errorCode, message, details, path);
    }
}
