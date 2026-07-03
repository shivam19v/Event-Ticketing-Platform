package com.eventsphere.event.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class EventSummaryResponse {
    private UUID id;
    private String title;
    private String category;
    private String imageUrl;
    private String status;
    private Instant startTime;
    private String city;
    private String venueName;
    private BigDecimal lowestPrice;
    private Integer totalAvailable;
}
