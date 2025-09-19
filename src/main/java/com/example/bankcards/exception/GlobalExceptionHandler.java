package com.example.bankcards.exception;

import com.example.bankcards.dto.response.error.ApiErrorResponse;
import com.example.bankcards.enums.ErrorCode;
import com.example.bankcards.exception.badrequest.BadRequestException;
import com.example.bankcards.exception.conflict.ConflictException;
import com.example.bankcards.exception.notfound.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            List<String> details,
            HttpServletRequest request
    ) {
        ApiErrorResponse apiErrorResponse = ApiErrorResponse.of(
                errorCode,
                message,
                details,
                request.getRequestURI()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(apiErrorResponse);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(errorCode, message, List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .toList();

        log.warn("Validation failed on {}: fields {}", request.getRequestURI(), details);
        return buildResponse(ErrorCode.BAD_REQUEST, "Validation failed", details, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad credentials on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class})
    public <T extends Exception> ResponseEntity<ApiErrorResponse> handleBadRequest(
            T ex, HttpServletRequest request
    ) {
        log.warn("Bad request on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Unauthorized access attempt on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.UNAUTHORIZED, "Authentication required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden access on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInternalServerError(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }
}
