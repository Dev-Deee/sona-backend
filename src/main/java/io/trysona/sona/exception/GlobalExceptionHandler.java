package io.trysona.sona.exception;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldIssue> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String message = details.isEmpty()
                ? "Request validation failed"
                : details.getFirst().field() + " " + details.getFirst().message();
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("VALIDATION_ERROR", message, details));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex) {
        List<ApiError.FieldIssue> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(err -> new ApiError.FieldIssue(
                                result.getMethodParameter().getParameterName(), err.getDefaultMessage())))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler({BadRequestException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST,
                ApiError.of("BAD_REQUEST", "Invalid value for parameter '" + ex.getName() + "'"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, ApiError.of("MALFORMED_REQUEST", "Request body is missing or malformed"));
    }

    @ExceptionHandler({EntityNotFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiError> handleNotFound(Exception ex) {
        String message = ex instanceof EntityNotFoundException ? ex.getMessage() : "Resource not found";
        return respond(HttpStatus.NOT_FOUND, ApiError.of("NOT_FOUND", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return respond(HttpStatus.FORBIDDEN, ApiError.of("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException ex) {
        return respond(HttpStatus.CONFLICT, ApiError.of("DUPLICATE_EMAIL", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ApiError.of("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(GoneException.class)
    ResponseEntity<ApiError> handleGone(GoneException ex) {
        return respond(HttpStatus.GONE, ApiError.of("GONE", ex.getMessage()));
    }

    @ExceptionHandler(ExpiredTokenException.class)
    ResponseEntity<ApiError> handleExpiredToken(ExpiredTokenException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ApiError.of("TOKEN_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ApiError.of("INVALID_TOKEN", ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    ResponseEntity<ApiError> handleBadCredentials(RuntimeException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ApiError.of("INVALID_CREDENTIALS", "Invalid email or password"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ApiError.of("METHOD_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Full detail goes to the server log only; clients get a generic message.
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                ApiError.of("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, ApiError body) {
        return ResponseEntity.status(status).body(body);
    }
}
