package com.eventsphere.event.repository;

import com.eventsphere.event.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    List<TicketType> findByEventId(UUID eventId);

    @Modifying
    @Query("UPDATE TicketType t SET t.sold = t.sold + :qty WHERE t.id = :id AND (t.quantity - t.sold) >= :qty")
    int incrementSold(@Param("id") UUID id, @Param("qty") int qty);
}
