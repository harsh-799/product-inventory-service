package com.harsh.product.inventory.exception;

import com.harsh.product.inventory.dto.response.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorDetails> handleUserAlreadyExistException(UserAlreadyExistsException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDetails> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message("Invalid credentials")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleProductNotFoundException(ProductNotFoundException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorDetails> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<ErrorDetails> handleTokenAlreadyUsedException(TokenAlreadyUsedException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

    @ExceptionHandler(TokenAlreadyExpiredException.class)
    public ResponseEntity<ErrorDetails> handleTokenAlreadyExpiredException(TokenAlreadyExpiredException ex) {
        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        java.util.List<com.harsh.product.inventory.dto.response.ValidationResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new com.harsh.product.inventory.dto.response.ValidationResponse(err.getField(), err.getDefaultMessage()))
                .toList();

        ErrorDetails errorDetails = ErrorDetails.builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }
}
