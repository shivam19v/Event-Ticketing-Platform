package com.eventsphere.event.controller;

import com.eventsphere.event.dto.*;
import com.eventsphere.event.entity.SeatMap;
import com.eventsphere.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/events") @RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> searchEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(eventService.searchEvents(status, category, city, search, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(eventService.createEvent(req, UUID.fromString(auth.getName())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable UUID id,
            @Valid @RequestBody CreateEventRequest req, Authentication auth) {
        return ResponseEntity.ok(eventService.updateEvent(id, req, UUID.fromString(auth.getName())));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(eventService.publishEvent(id, UUID.fromString(auth.getName())));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(eventService.cancelEvent(id, UUID.fromString(auth.getName())));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatMap>> getSeatAvailability(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getSeatAvailability(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventSummaryResponse>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(UUID.fromString(auth.getName()), PageRequest.of(page, size)));
    }
}
