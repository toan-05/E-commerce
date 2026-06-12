package com.example.order_service.exception;

import com.example.order_service.dto.response.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorResponse(
                        HttpStatus.CONFLICT,
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        HttpStatus.NOT_FOUND,
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldErrorResponse> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_FAILED,
                        "Validation failed",
                        request.getRequestURI(),
                        details
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(errorResponse(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.MALFORMED_REQUEST,
                        "Request body is missing or malformed",
                        request.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleEndpointNotFound(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.ENDPOINT_NOT_FOUND,
                        "Endpoint not found",
                        request.getRequestURI(),
                        List.of()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception for path={}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Unexpected server error",
                        request.getRequestURI(),
                        List.of()
                ));
    }

    private ApiErrorResponse errorResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            String path,
            List<ApiErrorResponse.FieldErrorResponse> details
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
}
