package com.ramani.banking.user.service;

import com.ramani.banking.user.entity.AuditLog;
import com.ramani.banking.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(UUID userId, String action, String entityType, UUID entityId,
                    String ipAddress, Map<String, Object> metadata) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .metadata(metadata)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write audit log for action={} userId={}: {}", action, userId, e.getMessage());
        }
    }
}
