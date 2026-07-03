package com.eventsphere.booking.service;

import com.eventsphere.booking.dto.ReserveRequest;
import com.eventsphere.booking.entity.Booking;
import com.eventsphere.booking.entity.Reservation;
import com.eventsphere.booking.exception.SeatAlreadyLockedException;
import com.eventsphere.booking.repository.BookingRepository;
import com.eventsphere.booking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Mock private ReservationRepository reservationRepo;
    @Mock private BookingRepository bookingRepo;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(bookingService, "reservationTimeoutMinutes", 15L);
    }

    @Test
    void reserveSeats_acquiresLocksAndCreatesReservation_whenSeatsAvailable() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID seat1 = UUID.randomUUID();
        UUID seat2 = UUID.randomUUID();

        ReserveRequest req = new ReserveRequest();
        req.setEventId(eventId);
        req.setTicketTypeId(UUID.randomUUID());
        req.setQuantity(2);
        req.setSeatIds(List.of(seat1, seat2));
        req.setTotalPrice(BigDecimal.valueOf(100));

        // Both locks succeed
        when(valueOperations.setIfAbsent(anyString(), eq(userId.toString()), any(Duration.class)))
                .thenReturn(true);

        Reservation savedReservation = Reservation.builder()
                .id(UUID.randomUUID()).userId(userId).eventId(eventId)
                .status("PENDING").totalPrice(BigDecimal.valueOf(100))
                .expiresAt(java.time.Instant.now().plusSeconds(900))
                .build();
        when(reservationRepo.save(any(Reservation.class))).thenReturn(savedReservation);

        Booking savedBooking = Booking.builder().id(UUID.randomUUID()).build();
        when(bookingRepo.save(any(Booking.class))).thenReturn(savedBooking);

        var result = bookingService.reserveSeats(userId, req);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(valueOperations, times(2)).setIfAbsent(anyString(), eq(userId.toString()), any(Duration.class));
        verify(reservationRepo, times(1)).save(any(Reservation.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("eventsphere.bookings"), eq("booking.created"), any(Object.class));
    }

    @Test
    void reserveSeats_throwsAndRollsBackLocks_whenSecondSeatAlreadyLocked() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID seat1 = UUID.randomUUID();
        UUID seat2 = UUID.randomUUID();

        ReserveRequest req = new ReserveRequest();
        req.setEventId(eventId);
        req.setTicketTypeId(UUID.randomUUID());
        req.setQuantity(2);
        req.setSeatIds(List.of(seat1, seat2));
        req.setTotalPrice(BigDecimal.valueOf(100));

        // First lock succeeds, second fails (already taken by another user)
        when(valueOperations.setIfAbsent(anyString(), eq(userId.toString()), any(Duration.class)))
                .thenReturn(true)   // seat1 lock acquired
                .thenReturn(false); // seat2 already locked

        assertThrows(SeatAlreadyLockedException.class, () -> bookingService.reserveSeats(userId, req));

        // Verify rollback: the first lock that succeeded gets released
        verify(redisTemplate, times(1)).delete(anyString());
        verify(reservationRepo, never()).save(any());
    }

    @Test
    void confirmBooking_updatesStatusToConfirmed_whenAwaitingPayment() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .id(bookingId).userId(userId).reservationId(reservationId)
                .bookingStatus("AWAITING_PAYMENT").totalPrice(BigDecimal.valueOf(50))
                .eventId(UUID.randomUUID())
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId).status("PENDING").totalPrice(BigDecimal.valueOf(50))
                .eventId(booking.getEventId())
                .expiresAt(java.time.Instant.now().plusSeconds(600))
                .build();

        when(bookingRepo.findByIdAndUserId(bookingId, userId)).thenReturn(java.util.Optional.of(booking));
        when(reservationRepo.findById(reservationId)).thenReturn(java.util.Optional.of(reservation));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = bookingService.confirmBooking(bookingId, paymentId, userId);

        assertEquals("CONFIRMED", result.getBookingStatus());
        assertEquals(paymentId, result.getPaymentId());
        verify(rabbitTemplate).convertAndSend(eq("eventsphere.bookings"), eq("booking.confirmed"), any(Object.class));
    }

    @Test
    void expireStaleReservations_releasesLocksAndMarksExpired() {
        UUID eventId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        Reservation expired = Reservation.builder()
                .id(UUID.randomUUID()).eventId(eventId)
                .seatIds(seatId.toString())
                .status("PENDING")
                .expiresAt(java.time.Instant.now().minusSeconds(60))
                .totalPrice(BigDecimal.TEN)
                .build();

        when(reservationRepo.findByStatusAndExpiresAtBefore(eq("PENDING"), any()))
                .thenReturn(List.of(expired));
        when(reservationRepo.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.findByReservationId(expired.getId())).thenReturn(java.util.Optional.empty());

        bookingService.expireStaleReservations();

        verify(redisTemplate).delete("seat:lock:" + eventId + ":" + seatId);
        verify(reservationRepo).save(argThat(r -> "EXPIRED".equals(r.getStatus())));
    }
}
