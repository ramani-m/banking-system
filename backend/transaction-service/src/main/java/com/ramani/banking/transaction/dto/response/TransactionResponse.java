package com.ramani.banking.transaction.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private UUID id;
    private String idempotencyKey;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String currency;
    private String type;
    private String status;
    private String description;
    private String referenceNumber;
    private String failureReason;
    private Map<String, Object> metadata;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
