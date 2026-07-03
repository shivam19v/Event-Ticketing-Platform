package com.eventsphere.ticket.dto;

import lombok.*;

@Data @Builder
public class ValidateTicketResponse {
    private String status;
    private String ticketNumber;
    private String message;
}
