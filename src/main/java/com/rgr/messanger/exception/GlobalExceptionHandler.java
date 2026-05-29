package com.rgr.messanger.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice   //глобальный обработчик для REST-контроллеров
public class GlobalExceptionHandler {

    // ========================
    // ВАЛИДАЦИЯ
    // ========================

    //аннотация на конкретный метод
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(err ->
                errors.put(err.getField(), err.getDefaultMessage())
        );

        ex.getBindingResult().getGlobalErrors().forEach(err ->
                errors.put(err.getObjectName(), err.getDefaultMessage())
        );

        return jsonResponse(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException e
    ) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(
            IllegalStateException e
    ) {
        log.warn("IllegalStateException: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(
            HttpMessageNotReadableException e
    ) {
        log.warn("Invalid JSON: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Неверный формат запроса");
    }

    // ========================
    // АУТЕНТИФИКАЦИЯ / ДОСТУП
    // ========================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException e
    ) {
        log.warn("BadCredentials: {}", e.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Неверные учетные данные пользователя");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(
            AuthenticationException e
    ) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Ошибка аутентификации");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException e
    ) {
        log.warn("AccessDenied: {}", e.getMessage());
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<Map<String, String>> handleEmailVerification(
            EmailVerificationException e
    ) {
        log.warn("EmailVerification: {}", e.getMessage());
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // ========================
    // NOT FOUND
    // ========================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            ResourceNotFoundException e
    ) {
        log.warn("ResourceNotFound: {}", e.getMessage());
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.debug("Static resource not found on [{}]: {}", request.getRequestURI(), e.getMessage());
        return error(HttpStatus.NOT_FOUND, "Ресурс не найден");
    }

    // ========================
    // ФАЙЛЫ / СТРИМИНГ
    // ========================
    @ExceptionHandler(ClientAbortException.class)
    public ResponseEntity<Void> handleClientAbort(
            ClientAbortException e,
            HttpServletRequest request
    ) {
        log.debug("Client aborted request on [{}]: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSize(
            MaxUploadSizeExceededException e
    ) {
        log.warn("File too large: {}", e.getMessage());
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Файл слишком большой");
    }

    // ========================
    // ПРОЧЕЕ
    // ========================
    @ExceptionHandler(ResourceMappingException.class)
    public ResponseEntity<Map<String, String>> handleMapping(
            ResourceMappingException e
    ) {
        log.error("ResourceMapping: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка обработки данных");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(
            DataIntegrityViolationException e
    ) {
        log.error("Data integrity: {}", e.getMessage(), e);
        return error(HttpStatus.CONFLICT, "Конфликт данных (возможно, дубликат)");
    }

    // ========================
    // ПОСЛЕДНИЙ РУБЕЖ
    // ========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
    }

    // ========================
    // ХЕЛПЕРЫ
    // ========================
    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message
    ) {
        return jsonResponse(
                status,
                Map.of("error", message != null ? message : status.getReasonPhrase())
        );
    }

    private ResponseEntity<Map<String, String>> jsonResponse(
            HttpStatus status,
            Map<String, String> body
    ) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}