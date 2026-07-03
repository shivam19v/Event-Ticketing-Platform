package com.eventsphere.notification.repository;

import com.eventsphere.notification.entity.SentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SentNotificationRepository extends JpaRepository<SentNotification, UUID> {}
