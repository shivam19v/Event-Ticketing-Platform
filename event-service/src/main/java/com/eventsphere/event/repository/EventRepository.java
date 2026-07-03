package com.eventsphere.event.repository;

import com.eventsphere.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    // Empty string means "no filter" — avoids null params entirely so Hibernate
    // never has to guess the SQL type. The service layer converts null → "".
    @Query(value = """
        SELECT e.* FROM event_schema.events e
        LEFT JOIN event_schema.venues v ON e.id = v.event_id
        WHERE (:status   = '' OR e.status   = :status)
          AND (:category = '' OR e.category = :category)
          AND (:city     = '' OR LOWER(v.city)  = LOWER(:city))
          AND (:search   = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY e.start_time ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM event_schema.events e
        LEFT JOIN event_schema.venues v ON e.id = v.event_id
        WHERE (:status   = '' OR e.status   = :status)
          AND (:category = '' OR e.category = :category)
          AND (:city     = '' OR LOWER(v.city)  = LOWER(:city))
          AND (:search   = '' OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        nativeQuery = true)
    Page<Event> searchEvents(@Param("status")   String status,
                             @Param("category") String category,
                             @Param("city")     String city,
                             @Param("search")   String search,
                             Pageable pageable);

    Page<Event> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId, Pageable pageable);
}
