package com.ramani.banking.notification.controller;

import com.ramani.banking.notification.dto.NotificationPreferencesRequest;
import com.ramani.banking.notification.dto.NotificationPreferencesResponse;
import com.ramani.banking.notification.dto.NotificationResponse;
import com.ramani.banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications and preferences")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications (paginated, newest first)")
    public Page<NotificationResponse> getMyNotifications(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getMyNotifications(UUID.fromString(userId), pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal String userId) {
        return Map.of("unreadCount", notificationService.getUnreadCount(UUID.fromString(userId)));
    }

    @PutMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark a single notification as read")
    public void markAsRead(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId, UUID.fromString(userId));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public Map<String, Integer> markAllAsRead(@AuthenticationPrincipal String userId) {
        int updated = notificationService.markAllAsRead(UUID.fromString(userId));
        return Map.of("markedRead", updated);
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get my notification preferences")
    public NotificationPreferencesResponse getPreferences(@AuthenticationPrincipal String userId) {
        return notificationService.getPreferences(UUID.fromString(userId));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update my notification preferences")
    public NotificationPreferencesResponse updatePreferences(
            @AuthenticationPrincipal String userId,
            @RequestBody NotificationPreferencesRequest request) {
        return notificationService.updatePreferences(UUID.fromString(userId), request);
    }
}
