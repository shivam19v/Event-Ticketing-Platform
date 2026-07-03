package com.eventsphere.ticket.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class TicketResponse {
    private UUID id;
    private String ticketNumber;
    private String status;
    private String qrCodeBase64;
    private UUID eventId;
    private UUID bookingId;
    private Instant issuedAt;
}
