package com.eventsphere.ticket.controller;

import com.eventsphere.ticket.dto.*;
import com.eventsphere.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/api/v1/tickets/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable UUID ticketId, Authentication auth) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId, UUID.fromString(auth.getName())));
    }

    @GetMapping("/api/v1/users/{userId}/tickets")
    public ResponseEntity<?> getUserTickets(@PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        if (!userId.equals(UUID.fromString(auth.getName()))) {
            return ResponseEntity.status(403).build();
        }
        Page<TicketResponse> tickets = ticketService.getUserTickets(userId, page, size);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/api/v1/tickets/{ticketNumber}/validate")
    public ResponseEntity<ValidateTicketResponse> validateTicket(
            @PathVariable String ticketNumber,
            @Valid @RequestBody ValidateTicketRequest req,
            Authentication auth) {
        UUID staffId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ticketService.validateTicket(ticketNumber, req, staffId));
    }
}
