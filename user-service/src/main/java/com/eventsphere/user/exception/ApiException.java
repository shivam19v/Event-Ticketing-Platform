package com.eventsphere.user.exception;
import org.springframework.http.HttpStatus;
import lombok.Getter;
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(String message, HttpStatus status) { super(message); this.status = status; }
}
