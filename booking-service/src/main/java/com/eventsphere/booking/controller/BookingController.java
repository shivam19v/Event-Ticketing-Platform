package com.eventsphere.booking.controller;
import com.eventsphere.booking.dto.*;
import com.eventsphere.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/bookings") @RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveSeats(
            @Valid @RequestBody ReserveRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.reserveSeats(userId, req));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID bookingId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getBooking(bookingId, userId));
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable UUID bookingId,
            @Valid @RequestBody ConfirmBookingRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId, req.getPaymentId(), userId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID bookingId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<Page<BookingResponse>> getUserBookings(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID currentUser = UUID.fromString(auth.getName());
        if (!userId.equals(currentUser)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(bookingService.getUserBookings(userId, page, size));
    }
}
