package com.eventsphere.booking.exception;
import org.springframework.http.HttpStatus;
public class SeatAlreadyLockedException extends ApiException {
    public SeatAlreadyLockedException(String msg) { super(msg, HttpStatus.CONFLICT); }
}
