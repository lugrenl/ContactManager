package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.AppErrorResponse;
import org.example.exceptions.ContactNotFoundException;
import org.example.exceptions.IncorrectCredentialsException;
import org.example.exceptions.UserAlreadyExistsException;
import org.example.dto.ValidationErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return ResponseEntity
                .badRequest()
                .body(new ValidationErrorResponse("Validation failed", System.currentTimeMillis(), errors));
    }

    @ExceptionHandler({ContactNotFoundException.class})
    private ResponseEntity<AppErrorResponse> handleContactNotFoundException(ContactNotFoundException e) {
        AppErrorResponse response = new AppErrorResponse(e.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<AppErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        AppErrorResponse response = new AppErrorResponse(e.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IncorrectCredentialsException.class)
    public ResponseEntity<AppErrorResponse> handleIncorrectCredentialsException(IncorrectCredentialsException e) {
        AppErrorResponse response = new AppErrorResponse(e.getMessage(), System.currentTimeMillis());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException exception, Locale locale) {
        ProblemDetail problemDetail = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST,
                        this.messageSource.getMessage("errors.400.title", new Object[0],
                                "errors.400.title", locale));
        problemDetail.setProperty("errors",
                exception.getAllErrors().stream()
                        .map(ObjectError::getDefaultMessage)
                        .toList());

        return ResponseEntity.badRequest()
                .body(problemDetail);
    }
}