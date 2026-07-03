package com.eventsphere.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class CreateEventRequest {
    @NotBlank @Size(min = 3, max = 255) private String title;
    private String description;
    private String category;
    private String imageUrl;
    @NotNull private Instant startTime;
    @NotNull private Instant endTime;
    @NotNull @Valid private VenueDto venue;
    @NotEmpty @Valid private List<TicketTypeDto> ticketTypes;

    @Data
    public static class VenueDto {
        @NotBlank private String name;
        private String address;
        @NotBlank private String city;
        private String state;
        private String country;
        private BigDecimal latitude;
        private BigDecimal longitude;
        @Min(1) private int capacity;
    }

    @Data
    public static class TicketTypeDto {
        @NotBlank private String name;
        private String description;
        @NotNull @DecimalMin("0.0") private BigDecimal price;
        @Min(1) private int quantity;
        private Instant saleStartTime;
        private Instant saleEndTime;
    }
}
