package com.eventsphere.ticket.service;

import com.eventsphere.ticket.dto.*;
import com.eventsphere.ticket.entity.*;
import com.eventsphere.ticket.exception.ApiException;
import com.eventsphere.ticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketScanRepository scanRepo;
    private final QRCodeGenerator qrGenerator;

    private static final int DUPLICATE_SCAN_WINDOW_SECONDS = 10;

    @Transactional
    public List<Ticket> issueTickets(UUID bookingId, UUID eventId, UUID userId, int quantity) {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String ticketNumber = generateTicketNumber(eventId);
            String qrData = "EVENTSPHERE:" + ticketNumber + ":" + UUID.randomUUID();
            Ticket ticket = Ticket.builder()
                .bookingId(bookingId).eventId(eventId).userId(userId)
                .ticketNumber(ticketNumber).qrCodeData(qrData).status("VALID").build();
            tickets.add(ticketRepo.save(ticket));
        }
        log.info("Issued {} tickets for booking {}", quantity, bookingId);
        return tickets;
    }

    public TicketResponse getTicket(UUID id, UUID userId) {
        Ticket t = ticketRepo.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ApiException("Ticket not found", HttpStatus.NOT_FOUND));
        return toResponse(t);
    }

    public Page<TicketResponse> getUserTickets(UUID userId, int page, int size) {
        return ticketRepo.findByUserIdOrderByIssuedAtDesc(userId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    @Transactional
    public ValidateTicketResponse validateTicket(String ticketNumber, ValidateTicketRequest req, UUID scannedBy) {
        Ticket ticket = ticketRepo.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null) {
            return ValidateTicketResponse.builder().status("INVALID")
                .ticketNumber(ticketNumber).message("Ticket not found").build();
        }
        if ("CANCELLED".equals(ticket.getStatus())) {
            recordScan(ticket, scannedBy, req, "INVALID");
            return ValidateTicketResponse.builder().status("INVALID")
                .ticketNumber(ticketNumber).message("Ticket has been cancelled").build();
        }

        // Duplicate scan detection: same ticket within window
        if (ticket.getLastUsedAt() != null) {
            long secondsSince = ChronoUnit.SECONDS.between(ticket.getLastUsedAt(), Instant.now());
            if (secondsSince < DUPLICATE_SCAN_WINDOW_SECONDS) {
                recordScan(ticket, scannedBy, req, "DUPLICATE");
                return ValidateTicketResponse.builder().status("DUPLICATE")
                    .ticketNumber(ticketNumber)
                    .message("Ticket already scanned " + secondsSince + "s ago").build();
            }
        }

        Instant now = Instant.now();
        if (ticket.getFirstUsedAt() == null) ticket.setFirstUsedAt(now);
        ticket.setLastUsedAt(now);
        ticket.setStatus("USED");
        ticketRepo.save(ticket);
        recordScan(ticket, scannedBy, req, "SUCCESS");

        return ValidateTicketResponse.builder().status("VALID")
            .ticketNumber(ticketNumber).message("Entry approved").build();
    }

    private void recordScan(Ticket ticket, UUID scannedBy, ValidateTicketRequest req, String result) {
        scanRepo.save(TicketScan.builder()
            .ticketId(ticket.getId()).eventId(ticket.getEventId()).scannedBy(scannedBy)
            .location(req.getLocation()).deviceId(req.getDeviceId()).result(result).build());
    }

    private String generateTicketNumber(UUID eventId) {
        String shortEventId = eventId.toString().substring(0, 8).toUpperCase();
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "EVT-" + shortEventId + "-" + random;
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
            .id(t.getId()).ticketNumber(t.getTicketNumber()).status(t.getStatus())
            .qrCodeBase64(qrGenerator.generateBase64QRCode(t.getQrCodeData()))
            .eventId(t.getEventId()).bookingId(t.getBookingId()).issuedAt(t.getIssuedAt()).build();
    }
}
