package com.ramani.banking.transaction.dto.request;

import com.ramani.banking.transaction.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {

    @NotNull(message = "Source account is required")
    private UUID fromAccountId;

    @NotNull(message = "Destination account is required")
    private UUID toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.9999", message = "Amount exceeds maximum allowed")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency = "USD";

    @Size(max = 200)
    private String description;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    private String idempotencyKey;

    private TransactionType type = TransactionType.P2P_TRANSFER;
}
