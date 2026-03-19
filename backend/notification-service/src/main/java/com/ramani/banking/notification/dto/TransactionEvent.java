package com.ramani.banking.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Matches the payload published by transaction-service to the "transaction-events" Kafka topic.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionEvent {
    private UUID transactionId;
    private String status;
    private BigDecimal amount;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID initiatedBy;
    private String currency;
    private String description;
    private String referenceNumber;
}
