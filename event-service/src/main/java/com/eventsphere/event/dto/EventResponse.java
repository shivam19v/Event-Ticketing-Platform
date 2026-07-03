package com.eventsphere.event.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class EventResponse {
    private UUID id;
    private UUID organizerId;
    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private Instant createdAt;
    private VenueDto venue;
    private List<TicketTypeDto> ticketTypes;
    private Integer totalCapacity;
    private Integer totalSold;

    @Data @Builder
    public static class VenueDto {
        private UUID id;
        private String name;
        private String address;
        private String city;
        private String state;
        private String country;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer capacity;
    }

    @Data @Builder
    public static class TicketTypeDto {
        private UUID id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer quantity;
        private Integer sold;
        private Integer available;
        private Instant saleStartTime;
        private Instant saleEndTime;
    }
}
