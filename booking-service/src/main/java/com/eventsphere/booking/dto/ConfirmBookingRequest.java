package com.eventsphere.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ConfirmBookingRequest {
    @NotNull private UUID paymentId;
}
