package com.eventsphere.booking.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ReserveRequest {
    @NotNull private UUID eventId;
    @NotNull private UUID ticketTypeId;
    @Min(1) @Max(10) private int quantity;
    private List<UUID> seatIds;
    @NotNull @DecimalMin("0.0") private BigDecimal totalPrice;
}
