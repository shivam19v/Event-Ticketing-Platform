package com.eventsphere.ticket.repository;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<com.eventsphere.ticket.entity.ProcessedEvent, String> {
}
