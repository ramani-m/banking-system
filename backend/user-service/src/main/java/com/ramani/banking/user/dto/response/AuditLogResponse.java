package com.ramani.banking.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID id;
    private String action;
    private String entityType;
    private UUID entityId;
    private String ipAddress;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
}
