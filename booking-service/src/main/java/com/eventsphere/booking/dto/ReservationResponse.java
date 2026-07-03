package com.eventsphere.booking.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ReservationResponse {
    private UUID reservationId;
    private UUID bookingId;
    private UUID eventId;
    private UUID ticketTypeId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private Instant expiresAt;
}
