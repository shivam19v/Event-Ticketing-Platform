package com.eventsphere.booking.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class BookingResponse {
    private UUID id;
    private UUID reservationId;
    private UUID eventId;
    private UUID ticketTypeId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String bookingStatus;
    private UUID paymentId;
    private Instant createdAt;
    private Instant completedAt;
}
