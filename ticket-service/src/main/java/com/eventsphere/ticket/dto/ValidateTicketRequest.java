package com.eventsphere.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTicketRequest {
    @NotBlank private String location;
    private String deviceId;
}
