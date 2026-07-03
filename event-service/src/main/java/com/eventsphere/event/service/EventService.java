package com.eventsphere.event.service;

import com.eventsphere.event.dto.*;
import com.eventsphere.event.entity.*;
import com.eventsphere.event.exception.ApiException;
import com.eventsphere.event.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class EventService {

    private final EventRepository eventRepo;
    private final SeatMapRepository seatMapRepo;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public EventResponse createEvent(CreateEventRequest req, UUID organizerId) {
        Event event = Event.builder()
            .organizerId(organizerId).title(req.getTitle()).description(req.getDescription())
            .category(req.getCategory()).imageUrl(req.getImageUrl())
            .startTime(req.getStartTime()).endTime(req.getEndTime()).status("DRAFT").build();

        var vDto = req.getVenue();
        Venue venue = Venue.builder().event(event)
            .name(vDto.getName()).address(vDto.getAddress()).city(vDto.getCity())
            .state(vDto.getState()).country(vDto.getCountry())
            .latitude(vDto.getLatitude()).longitude(vDto.getLongitude())
            .capacity(vDto.getCapacity()).build();
        event.setVenue(venue);

        List<TicketType> types = req.getTicketTypes().stream().map(t ->
            TicketType.builder().event(event).name(t.getName()).description(t.getDescription())
                .price(t.getPrice()).quantity(t.getQuantity())
                .saleStartTime(t.getSaleStartTime()).saleEndTime(t.getSaleEndTime()).build()
        ).toList();
        event.setTicketTypes(types);

        Event saved = eventRepo.save(event);
        log.info("Event created: {} by organizer: {}", saved.getId(), organizerId);
        return toResponse(saved);
    }

    @Cacheable(value = "events", key = "#id")
    public EventResponse getEvent(UUID id) {
        return eventRepo.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ApiException("Event not found", HttpStatus.NOT_FOUND));
    }

    public Page<EventSummaryResponse> searchEvents(String status, String category, String city, String search, Pageable pageable) {
        // Convert nulls to empty string — repository uses "" as "no filter" sentinel
        // so Hibernate always sends a typed String param, never a null/bytea.
        String s = (status   != null && !status.isBlank())   ? status   : "PUBLISHED";
        String c = (category != null && !category.isBlank()) ? category : "";
        String ci = (city    != null && !city.isBlank())     ? city     : "";
        String se = (search  != null && !search.isBlank())   ? search   : "";
        return eventRepo.searchEvents(s, c, ci, se, pageable).map(this::toSummary);
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse updateEvent(UUID id, CreateEventRequest req, UUID organizerId) {
        Event event = getEventOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId))
            throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        if ("CANCELLED".equals(event.getStatus()))
            throw new ApiException("Cannot update a cancelled event", HttpStatus.BAD_REQUEST);
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setCategory(req.getCategory());
        event.setImageUrl(req.getImageUrl());
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        return toResponse(eventRepo.save(event));
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse publishEvent(UUID id, UUID organizerId) {
        Event event = getEventOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId))
            throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        if (!"DRAFT".equals(event.getStatus()))
            throw new ApiException("Only DRAFT events can be published", HttpStatus.BAD_REQUEST);
        event.setStatus("PUBLISHED");
        Event saved = eventRepo.save(event);
        rabbitTemplate.convertAndSend("eventsphere.events", "event.published",
            Map.of("eventId", id.toString(), "title", event.getTitle()));
        return toResponse(saved);
    }

    @CacheEvict(value = "events", key = "#id")
    @Transactional
    public EventResponse cancelEvent(UUID id, UUID organizerId) {
        Event event = getEventOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId))
            throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        if ("CANCELLED".equals(event.getStatus()))
            throw new ApiException("Event already cancelled", HttpStatus.BAD_REQUEST);
        event.setStatus("CANCELLED");
        Event saved = eventRepo.save(event);
        rabbitTemplate.convertAndSend("eventsphere.events", "event.cancelled",
            Map.of("eventId", id.toString(), "title", event.getTitle()));
        return toResponse(saved);
    }

    public List<SeatMap> getSeatAvailability(UUID eventId) {
        getEventOrThrow(eventId);
        return seatMapRepo.findByEventId(eventId);
    }

    public Page<EventSummaryResponse> getOrganizerEvents(UUID organizerId, Pageable pageable) {
        return eventRepo.findByOrganizerIdOrderByCreatedAtDesc(organizerId, pageable).map(this::toSummary);
    }

    private Event getEventOrThrow(UUID id) {
        return eventRepo.findById(id)
            .orElseThrow(() -> new ApiException("Event not found", HttpStatus.NOT_FOUND));
    }

    private EventResponse toResponse(Event e) {
        Venue v = e.getVenue();
        return EventResponse.builder()
            .id(e.getId()).organizerId(e.getOrganizerId()).title(e.getTitle())
            .description(e.getDescription()).category(e.getCategory()).imageUrl(e.getImageUrl())
            .status(e.getStatus()).startTime(e.getStartTime()).endTime(e.getEndTime())
            .createdAt(e.getCreatedAt())
            .venue(v == null ? null : EventResponse.VenueDto.builder()
                .id(v.getId()).name(v.getName()).address(v.getAddress())
                .city(v.getCity()).state(v.getState()).country(v.getCountry())
                .latitude(v.getLatitude()).longitude(v.getLongitude())
                .capacity(v.getCapacity()).build())
            .ticketTypes(e.getTicketTypes().stream().map(t -> EventResponse.TicketTypeDto.builder()
                .id(t.getId()).name(t.getName()).description(t.getDescription()).price(t.getPrice())
                .quantity(t.getQuantity()).sold(t.getSold()).available(t.getQuantity() - t.getSold())
                .saleStartTime(t.getSaleStartTime()).saleEndTime(t.getSaleEndTime()).build()).toList())
            .totalCapacity(v == null ? 0 : v.getCapacity())
            .totalSold(e.getTicketTypes().stream().mapToInt(TicketType::getSold).sum())
            .build();
    }

    private EventSummaryResponse toSummary(Event e) {
        Venue v = e.getVenue();
        BigDecimal lowestPrice = e.getTicketTypes().stream()
            .map(TicketType::getPrice).min(BigDecimal::compareTo).orElse(null);
        int available = e.getTicketTypes().stream().mapToInt(t -> t.getQuantity() - t.getSold()).sum();
        return EventSummaryResponse.builder()
            .id(e.getId()).title(e.getTitle()).category(e.getCategory())
            .imageUrl(e.getImageUrl()).status(e.getStatus()).startTime(e.getStartTime())
            .city(v == null ? null : v.getCity()).venueName(v == null ? null : v.getName())
            .lowestPrice(lowestPrice).totalAvailable(available).build();
    }
}
