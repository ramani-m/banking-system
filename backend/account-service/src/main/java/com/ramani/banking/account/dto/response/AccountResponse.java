package com.ramani.banking.account.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private String type;
    private String currency;
    private String status;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalBalance;
    private BigDecimal dailyTransferLimit;
    private BigDecimal dailyTransferredToday;
    private LocalDateTime createdAt;
}
