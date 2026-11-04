package com.rbm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> business(BusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .reduce((a, b) -> a + " ; " + b).orElse("Données invalides");
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> fichierTropGros(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "La photo ne doit pas dépasser 2 Mo");
    }
    @ExceptionHandler(ForbiddenException.class)
public ResponseEntity<Map<String, Object>> interdit(ForbiddenException ex) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage());
}

@ExceptionHandler(AuthenticationFailedException.class)
public ResponseEntity<Map<String, Object>> authEchouee(AuthenticationFailedException ex) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
}

@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, Object>> jsonIllisible(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Requête illisible : vérifiez le format JSON et des dates (ex : 2026-12-01T14:00:00)");
}

@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> autre(Exception ex) {
    // Erreurs Spring MVC (404 route inconnue, 405 méthode...) : on garde leur code
    if (ex instanceof ErrorResponse er) {
        return build(HttpStatus.valueOf(er.getStatusCode().value()), ex.getMessage());
    }
    log.error("Erreur inattendue", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
}
}