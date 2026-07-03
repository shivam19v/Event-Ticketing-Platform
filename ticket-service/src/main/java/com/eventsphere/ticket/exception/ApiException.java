package com.eventsphere.ticket.exception;
import org.springframework.http.HttpStatus;
import lombok.Getter;
@Getter public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(String msg, HttpStatus s) { super(msg); this.status = s; }
}
