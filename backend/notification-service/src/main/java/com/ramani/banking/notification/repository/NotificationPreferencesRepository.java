package com.ramani.banking.notification.repository;

import com.ramani.banking.notification.entity.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, UUID> {
    Optional<NotificationPreferences> findByUserId(UUID userId);
}
