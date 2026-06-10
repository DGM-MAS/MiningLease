package com.mas.gov.bt.mas.primary.config;

import com.mas.gov.bt.mas.primary.utility.ApiErrorResponse;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Global Exception Handler.
 *
 * MAS-wide contract: ApiErrorResponse { errorCode, message, details, timestamp }.
 * `message`/`details` are always safe text — either self-authored business
 * exception messages or ErrorCodes descriptions. System exception messages
 * are logged, never returned.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("ResourceNotFoundException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ErrorCodes.RECORD_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("DuplicateResourceException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_ENTRY, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("InvalidCredentialsException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLockedException(
            AccountLockedException ex, HttpServletRequest request) {
        log.warn("AccountLockedException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_LOCKED, ex.getMessage());
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountInactiveException(
            AccountInactiveException ex, HttpServletRequest request) {
        log.warn("AccountInactiveException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_DISABLED, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedOperationException(
            UnauthorizedOperationException ex, HttpServletRequest request) {
        log.warn("UnauthorizedOperationException at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Bean Validation messages are self-authored on the DTOs — safe to return
        String fieldDetails = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error at {}: {}", request.getRequestURI(), fieldDetails);
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_INPUT_DATA, fieldDetails);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Invalid path/query parameter '{}': {}", ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_INPUT_DATA,
                "Parameter '" + ex.getName() + "' has an invalid value.");
    }

    @ExceptionHandler(com.mas.gov.bt.mas.primary.exception.BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            com.mas.gov.bt.mas.primary.exception.BusinessException ex, HttpServletRequest request) {
        log.warn("BusinessException [{}] at {}: {}", ex.getErrorCode(), request.getRequestURI(), ex.getDetails());
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(),
                ex.getDetails() != null ? ex.getDetails() : "Request could not be processed.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled RuntimeException at {}", request.getRequestURI(), ex);
        return build(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST,
                "An error occurred while processing your request.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String errorCode, String safeDetails) {
        ApiErrorResponse body = new ApiErrorResponse(
                errorCode,
                ErrorCodes.getErrorDescription(errorCode),
                safeDetails != null ? safeDetails : ErrorCodes.getErrorDescription(errorCode));
        return new ResponseEntity<>(body, status);
    }

    // Custom exception classes
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(String message) {
            super(message);
        }
    }

    public static class AccountInactiveException extends RuntimeException {
        public AccountInactiveException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedOperationException extends RuntimeException {
        public UnauthorizedOperationException(String message) {
            super(message);
        }
    }
}
