package com.example.order_service.dto.response.common;

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

    public record FieldErrorResponse(
            String field,
            String message
    ) {
    }
}
