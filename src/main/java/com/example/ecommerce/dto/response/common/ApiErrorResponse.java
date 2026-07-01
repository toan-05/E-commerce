package com.example.ecommerce.dto.response.common;

import com.example.ecommerce.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        Instant timestamp,
        int status,
        String code,
        String error,
        String message,
        String path,
        List<FieldErrorResponse> details
) {

    public static ApiErrorResponse of(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            String path,
            List<FieldErrorResponse> details
    ) {
        return new ApiErrorResponse(
                false,
                Instant.now(),
                status.value(),
                errorCode.name(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
    }

    public record FieldErrorResponse(
            String field,
            String message
    ) {
    }
}
