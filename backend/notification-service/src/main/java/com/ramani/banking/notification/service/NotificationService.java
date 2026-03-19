package com.ramani.banking.notification.service;

import com.ramani.banking.notification.dto.NotificationPreferencesRequest;
import com.ramani.banking.notification.dto.NotificationPreferencesResponse;
import com.ramani.banking.notification.dto.NotificationResponse;
import com.ramani.banking.notification.dto.TransactionEvent;
import com.ramani.banking.notification.entity.Notification;
import com.ramani.banking.notification.entity.NotificationChannel;
import com.ramani.banking.notification.entity.NotificationPreferences;
import com.ramani.banking.notification.entity.NotificationType;
import com.ramani.banking.notification.repository.NotificationPreferencesRepository;
import com.ramani.banking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final EmailService emailService;

    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        if (event.getInitiatedBy() == null) {
            log.warn("Transaction event missing initiatedBy — skipping: {}", event.getTransactionId());
            return;
        }

        UUID userId = event.getInitiatedBy();
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        NotificationType type = resolveType(event.getStatus());
        String title = buildTitle(type, event);
        String body = buildBody(event);
        Map<String, Object> metadata = Map.of(
                "transactionId", event.getTransactionId().toString(),
                "amount", event.getAmount() != null ? event.getAmount().toString() : "0",
                "status", event.getStatus()
        );

        if (prefs.isInAppEnabled()) {
            saveNotification(userId, type, title, body, NotificationChannel.IN_APP, metadata);
        }

        if (prefs.isEmailEnabled()) {
            saveNotification(userId, type, title, body, NotificationChannel.EMAIL, metadata);
            // Email address would normally come from auth-service; using placeholder for now
            emailService.sendEmail(userId + "@ramani-internal", title, body);
        }

        log.info("Notifications dispatched for transaction {} userId {}", event.getTransactionId(), userId);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllReadForUser(userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(UUID userId) {
        return toPreferencesResponse(getOrCreatePreferences(userId));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(UUID userId, NotificationPreferencesRequest request) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        if (request.getEmailEnabled() != null)  prefs.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled() != null)    prefs.setSmsEnabled(request.getSmsEnabled());
        if (request.getPushEnabled() != null)   prefs.setPushEnabled(request.getPushEnabled());
        if (request.getInAppEnabled() != null)  prefs.setInAppEnabled(request.getInAppEnabled());
        return toPreferencesResponse(preferencesRepository.save(prefs));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void saveNotification(UUID userId, NotificationType type, String title,
                                   String body, NotificationChannel channel,
                                   Map<String, Object> metadata) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .channel(channel)
                .metadata(metadata)
                .build());
    }

    private NotificationPreferences getOrCreatePreferences(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(
                        NotificationPreferences.builder().userId(userId).build()));
    }

    private NotificationType resolveType(String status) {
        if (status == null) return NotificationType.SYSTEM_ALERT;
        return switch (status.toUpperCase()) {
            case "COMPLETED" -> NotificationType.TRANSACTION_COMPLETED;
            case "FAILED"    -> NotificationType.TRANSACTION_FAILED;
            case "REVERSED"  -> NotificationType.TRANSACTION_REVERSED;
            default          -> NotificationType.SYSTEM_ALERT;
        };
    }

    private String buildTitle(NotificationType type, TransactionEvent event) {
        BigDecimal amount = event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO;
        String currency = event.getCurrency() != null ? event.getCurrency() : "USD";
        return switch (type) {
            case TRANSACTION_COMPLETED -> String.format("Transfer of %s %s completed", amount, currency);
            case TRANSACTION_FAILED    -> String.format("Transfer of %s %s failed", amount, currency);
            case TRANSACTION_REVERSED  -> String.format("Transfer of %s %s reversed", amount, currency);
            default                    -> "Transaction update";
        };
    }

    private String buildBody(TransactionEvent event) {
        BigDecimal amount = event.getAmount() != null ? event.getAmount() : BigDecimal.ZERO;
        String currency = event.getCurrency() != null ? event.getCurrency() : "USD";
        String desc = event.getDescription() != null ? " — " + event.getDescription() : "";
        return String.format(
                "Your transaction of %s %s is now %s%s.\nReference: %s",
                amount, currency,
                event.getStatus() != null ? event.getStatus().toLowerCase() : "updated",
                desc,
                event.getReferenceNumber() != null ? event.getReferenceNumber() : event.getTransactionId()
        );
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .body(n.getBody())
                .channel(n.getChannel().name())
                .read(n.isRead())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private NotificationPreferencesResponse toPreferencesResponse(NotificationPreferences p) {
        return NotificationPreferencesResponse.builder()
                .emailEnabled(p.isEmailEnabled())
                .smsEnabled(p.isSmsEnabled())
                .pushEnabled(p.isPushEnabled())
                .inAppEnabled(p.isInAppEnabled())
                .build();
    }
}
