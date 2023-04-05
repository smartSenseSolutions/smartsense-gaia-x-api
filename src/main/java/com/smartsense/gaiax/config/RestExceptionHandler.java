/*
 * Copyright (c) 2023 | smartSense
 */

package com.smartsense.gaiax.config;

import com.smartsense.gaiax.dto.CommonResponse;
import com.smartsense.gaiax.dto.ErrorResponse;
import com.smartsense.gaiax.dto.ValidationErrorResponse;
import com.smartsense.gaiax.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.SecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * The constant HANDLE_ENTITY_EXCEPTION_ERROR.
     */
    public static final String HANDLE_ENTITY_EXCEPTION_ERROR = "handleEntityException: Error";
    private static final String INTERNAL_SERVER_ERROR = "internal.server.error";
    private static final String AI_ML_ERROR = "aiml.server.error";
    private final MessageSource messageSource;
    private final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Instantiates a new Platform exception handler.
     *
     * @param messageSource the message source
     */
    @Autowired
    public RestExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handle exception response entity.
     *
     * @param exception the exception
     * @param request   the request
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Exception.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> handleException(Exception exception, HttpServletRequest request) {

        log.error("Internal server error, API={}, Method={}", request.getRequestURI(), request.getMethod(), exception);
        String message = messageSource.getMessage(INTERNAL_SERVER_ERROR, new Object[]{}, LocaleContextHolder.getLocale());

        Map<String, Object> map = new HashMap<>();
        map.put("error", new ErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResponse.builder(map).message(message).status(HttpStatus.INTERNAL_SERVER_ERROR.value()).build());
    }

    /**
     * Security exception response entity.
     *
     * @param exception the exception
     * @param request   the request
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({SecurityException.class, MissingRequestHeaderException.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> securityException(Exception exception, HttpServletRequest request) {
        log.error("SecurityException on api={}, method = {}", request.getRequestURI(), request.getMethod(), exception);
        Map<String, Object> map = new HashMap<>();
        String msg;
        try {
            msg = messageSource.getMessage(exception.getMessage(), null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            msg = exception.getMessage();
        }

        map.put("error", new ErrorResponse(msg, HttpStatus.UNAUTHORIZED.value()));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(CommonResponse.builder(map).message(msg).status(HttpStatus.UNAUTHORIZED.value()).build());
    }


    /**
     * Handle entity exception response entity.
     *
     * @param exception the exception
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class, DuplicateEntityException.class, EntityCreationException.class, BadDataException.class, EntityModificationException.class, HttpRequestMethodNotSupportedException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> handleEntityException(Exception exception) {
        log.error(HANDLE_ENTITY_EXCEPTION_ERROR, exception.getMessage());
        String msg;
        try {
            msg = messageSource.getMessage(exception.getMessage(), null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            if (exception instanceof HttpMessageNotReadableException) {
                msg = "Invalid data";
            } else {
                msg = exception.getMessage();
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put("error", new ErrorResponse(msg, HttpStatus.BAD_REQUEST.value()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.builder(map).message(msg).status(HttpStatus.BAD_REQUEST.value()).build());
    }

    /**
     * Handle not found response entity.
     *
     * @param exception the exception
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({EntityNotFoundException.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> handleNotFound(Exception exception) {
        log.error(HANDLE_ENTITY_EXCEPTION_ERROR, exception);
        Map<String, Object> map = new HashMap<>();
        map.put("error", new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND.value()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResponse.builder(map).message(exception.getMessage()).status(HttpStatus.NOT_FOUND.value()).build());
    }

    /**
     * Handle validation response entity.
     *
     * @param exception the exception
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getAllErrors().stream().map(error -> (FieldError) error).collect(Collectors.toList());
        return handleValidationError(fieldErrors);

    }

    /**
     * Handle validation response entity.
     *
     * @param exception the exception
     * @return the response entity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<CommonResponse<Object>> handleValidation(ConstraintViolationException exception) {
        log.error(HANDLE_ENTITY_EXCEPTION_ERROR, exception.getMessage());
        List<String> fieldErrors = exception.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
        return ResponseEntity.badRequest().body(CommonResponse.builder(new Object()).message(fieldErrors.get(0)).status(HttpStatus.BAD_REQUEST.value()).build());
    }

    /**
     * Handle validation response entity.
     *
     * @param exception the exception
     * @return ResponseEntity with error details
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({BindException.class})
    public ResponseEntity<CommonResponse<Map<String, Object>>> handleValidation(BindException exception) {
        log.error(HANDLE_ENTITY_EXCEPTION_ERROR, exception.getMessage());
        List<FieldError> fieldErrors = exception.getBindingResult().getAllErrors().stream().map(error -> (FieldError) error).collect(Collectors.toList());
        return handleValidationError(fieldErrors);

    }

    private String getMessage(Exception exception) {
        String errorMessage;
        try {
            errorMessage = messageSource.getMessage(exception.getMessage(), null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            if (exception instanceof HttpMessageNotReadableException) {
                errorMessage = "Invalid data";
            } else {
                errorMessage = exception.getMessage();
            }
        }
        return errorMessage;
    }

    /**
     * @param fieldErrors errors
     * @return ResponseEntity with error details
     */
    private ResponseEntity<CommonResponse<Map<String, Object>>> handleValidationError(List<FieldError> fieldErrors) {

        Map<String, String> messages = new HashMap<>();
        fieldErrors.forEach(fieldError -> messages.put(fieldError.getField(), fieldError.getDefaultMessage()));
        Map<String, Object> map = new HashMap<>();
        map.put("error", new ValidationErrorResponse(messages, HttpStatus.BAD_REQUEST.value(), "Validation failed"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.builder(map).message(messages.entrySet().iterator().next().getValue()).status(HttpStatus.BAD_REQUEST.value()).build());
    }
}
