package com.ramani.banking.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String type;
    private String title;
    private String body;
    private String channel;
    private boolean read;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
