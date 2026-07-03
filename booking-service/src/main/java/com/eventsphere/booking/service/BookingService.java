package com.eventsphere.booking.service;

import com.eventsphere.booking.dto.*;
import com.eventsphere.booking.entity.*;
import com.eventsphere.booking.exception.*;
import com.eventsphere.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class BookingService {

    private final ReservationRepository reservationRepo;
    private final BookingRepository bookingRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${booking.reservation-timeout-minutes:15}")
    private long reservationTimeoutMinutes;

    private static final String SEAT_LOCK_PREFIX      = "seat:lock:";
    private static final String EVENT_QUOTA_PREFIX    = "event:quota:";
    private static final String QUOTA_ROLLBACK_PREFIX = "quota-rollback:";

    @Transactional
    public ReservationResponse reserveSeats(UUID userId, ReserveRequest req) {
        log.info("Reserving {} seats for user={} event={}", req.getQuantity(), userId, req.getEventId());

        List<String> acquiredLocks = new ArrayList<>();
        try {
            if (req.getSeatIds() != null && !req.getSeatIds().isEmpty()) {
                for (UUID seatId : req.getSeatIds()) {
                    String key = SEAT_LOCK_PREFIX + req.getEventId() + ":" + seatId;
                    Boolean locked = redisTemplate.opsForValue()
                        .setIfAbsent(key, userId.toString(), Duration.ofMinutes(reservationTimeoutMinutes));
                    if (!Boolean.TRUE.equals(locked)) {
                        releaseLocks(acquiredLocks);
                        throw new SeatAlreadyLockedException("Seat " + seatId + " is already reserved.");
                    }
                    acquiredLocks.add(key);
                }
            } else {
                String quotaKey = EVENT_QUOTA_PREFIX + req.getEventId() + ":" + req.getTicketTypeId();
                Long remaining = redisTemplate.opsForValue().decrement(quotaKey, req.getQuantity());
                if (remaining == null || remaining < 0) {
                    redisTemplate.opsForValue().increment(quotaKey, req.getQuantity());
                    throw new ApiException("Not enough tickets available", HttpStatus.CONFLICT);
                }
                acquiredLocks.add(QUOTA_ROLLBACK_PREFIX + quotaKey + "::" + req.getQuantity());
            }

            Instant expiresAt = Instant.now().plus(reservationTimeoutMinutes, ChronoUnit.MINUTES);
            String seatIdsStr = req.getSeatIds() != null
                ? req.getSeatIds().stream().map(UUID::toString).collect(Collectors.joining(","))
                : null;

            Reservation reservation = Reservation.builder()
                .userId(userId).eventId(req.getEventId()).ticketTypeId(req.getTicketTypeId())
                .quantity(req.getQuantity()).seatIds(seatIdsStr).totalPrice(req.getTotalPrice())
                .status("PENDING").expiresAt(expiresAt).build();
            reservation = reservationRepo.save(reservation);

            Booking booking = Booking.builder()
                .reservationId(reservation.getId()).userId(userId).eventId(req.getEventId())
                .ticketTypeId(req.getTicketTypeId()).quantity(req.getQuantity())
                .totalPrice(req.getTotalPrice()).bookingStatus("AWAITING_PAYMENT").build();
            Booking savedBooking = bookingRepo.save(booking);

            publishBookingEvent("booking.created", Map.of(
                "bookingId",     savedBooking.getId().toString(),
                "reservationId", reservation.getId().toString(),
                "userId",        userId.toString(),
                "eventId",       req.getEventId().toString(),
                "totalPrice",    req.getTotalPrice().toString(),
                "quantity",      String.valueOf(req.getQuantity())
            ));

            return ReservationResponse.builder()
                .reservationId(reservation.getId()).bookingId(savedBooking.getId())
                .eventId(req.getEventId()).ticketTypeId(req.getTicketTypeId())
                .quantity(req.getQuantity()).totalPrice(req.getTotalPrice())
                .status("PENDING").expiresAt(expiresAt).build();

        } catch (ApiException e) {
            releaseLocks(acquiredLocks);
            throw e;
        } catch (Exception e) {
            releaseLocks(acquiredLocks);
            log.error("Unexpected error during reservation", e);
            throw new ApiException("Reservation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, UUID paymentId, UUID userId) {
        Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
        if (!"AWAITING_PAYMENT".equals(booking.getBookingStatus()))
            throw new ApiException("Booking is not awaiting payment", HttpStatus.BAD_REQUEST);

        booking.setBookingStatus("CONFIRMED");
        booking.setPaymentId(paymentId);
        booking.setCompletedAt(Instant.now());

        Reservation reservation = reservationRepo.findById(booking.getReservationId())
            .orElseThrow(() -> new ApiException("Reservation not found", HttpStatus.NOT_FOUND));
        reservation.setStatus("CONFIRMED");
        reservation.setConfirmedAt(Instant.now());
        reservationRepo.save(reservation);

        Booking saved = bookingRepo.save(booking);
        publishBookingEvent("booking.confirmed", Map.of(
            "bookingId",  booking.getId().toString(),
            "userId",     userId.toString(),
            "eventId",    booking.getEventId().toString(),
            "paymentId",  paymentId.toString(),
            "totalPrice", booking.getTotalPrice().toString()
        ));
        return toResponse(saved);
    }

    @Transactional
    public void cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepo.findByIdAndUserId(bookingId, userId)
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
        if ("CANCELLED".equals(booking.getBookingStatus()))
            throw new ApiException("Booking already cancelled", HttpStatus.BAD_REQUEST);

        booking.setBookingStatus("CANCELLED");
        bookingRepo.save(booking);

        reservationRepo.findById(booking.getReservationId()).ifPresent(r -> {
            if ("PENDING".equals(r.getStatus())) {
                releaseSeatLocks(r);
                r.setStatus("CANCELLED");
                r.setCancelledAt(Instant.now());
                reservationRepo.save(r);
            }
        });

        publishBookingEvent("booking.cancelled", Map.of(
            "bookingId", booking.getId().toString(),
            "userId",    userId.toString(),
            "eventId",   booking.getEventId().toString()
        ));
    }

    public BookingResponse getBooking(UUID bookingId, UUID userId) {
        return bookingRepo.findByIdAndUserId(bookingId, userId)
            .map(this::toResponse)
            .orElseThrow(() -> new ApiException("Booking not found", HttpStatus.NOT_FOUND));
    }

    public Page<BookingResponse> getUserBookings(UUID userId, int page, int size) {
        return bookingRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    @Scheduled(fixedDelay = 300_000)
    public void expireStaleReservations() {
        List<Reservation> expired = reservationRepo.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        if (!expired.isEmpty()) log.info("Expiring {} stale reservations", expired.size());
        for (Reservation r : expired) {
            releaseSeatLocks(r);
            r.setStatus("EXPIRED");
            reservationRepo.save(r);
            bookingRepo.findByReservationId(r.getId()).ifPresent(b -> {
                if ("AWAITING_PAYMENT".equals(b.getBookingStatus())) {
                    b.setBookingStatus("EXPIRED");
                    bookingRepo.save(b);
                }
            });
        }
    }

    private void releaseSeatLocks(Reservation r) {
        if (r.getSeatIds() != null && !r.getSeatIds().isBlank()) {
            for (String seatId : r.getSeatIds().split(",")) {
                String trimmed = seatId.trim();
                if (!trimmed.isEmpty()) {
                    redisTemplate.delete(SEAT_LOCK_PREFIX + r.getEventId() + ":" + trimmed);
                }
            }
        }
    }

    private void releaseLocks(List<String> keys) {
        for (String k : keys) {
            if (k.startsWith(QUOTA_ROLLBACK_PREFIX)) {
                String without = k.substring(QUOTA_ROLLBACK_PREFIX.length());
                int sep = without.lastIndexOf("::");
                if (sep > 0) {
                    String quotaKey = without.substring(0, sep);
                    long qty = Long.parseLong(without.substring(sep + 2));
                    redisTemplate.opsForValue().increment(quotaKey, qty);
                }
            } else {
                redisTemplate.delete(k);
            }
        }
    }

    private void publishBookingEvent(String routingKey, Map<String, String> payload) {
        try {
            rabbitTemplate.convertAndSend("eventsphere.bookings", routingKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish {}: {}", routingKey, e.getMessage());
        }
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
            .id(b.getId()).reservationId(b.getReservationId())
            .eventId(b.getEventId()).ticketTypeId(b.getTicketTypeId())
            .quantity(b.getQuantity()).totalPrice(b.getTotalPrice())
            .bookingStatus(b.getBookingStatus()).paymentId(b.getPaymentId())
            .createdAt(b.getCreatedAt()).completedAt(b.getCompletedAt()).build();
    }
}
