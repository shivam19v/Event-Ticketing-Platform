package com.eventsphere.booking.repository;

import com.eventsphere.booking.entity.Booking;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Booking> findByIdAndUserId(UUID id, UUID userId);
    List<Booking> findByEventIdAndBookingStatus(UUID eventId, String status);
    Optional<Booking> findByReservationId(UUID reservationId);
}
