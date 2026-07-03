package com.eventsphere.event.repository;

import com.eventsphere.event.entity.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SeatMapRepository extends JpaRepository<SeatMap, UUID> {
    List<SeatMap> findByEventId(UUID eventId);
    List<SeatMap> findByEventIdAndStatus(UUID eventId, String status);
    long countByEventIdAndStatus(UUID eventId, String status);

    @Modifying
    @Query("UPDATE SeatMap s SET s.status = :status WHERE s.id IN :ids")
    void updateStatusByIds(@Param("ids") List<UUID> ids, @Param("status") String status);
}
