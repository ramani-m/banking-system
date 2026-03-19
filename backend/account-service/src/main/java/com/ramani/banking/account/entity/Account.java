package com.ramani.banking.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_accounts_account_number", columnList = "account_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "daily_transfer_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransferLimit = new BigDecimal("10000.00");

    @Column(name = "daily_transferred_today", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyTransferredToday = BigDecimal.ZERO;

    @Column(name = "daily_limit_reset_at")
    private LocalDateTime dailyLimitResetAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BigDecimal getTotalBalance() {
        return availableBalance.add(pendingBalance);
    }

    public boolean canTransfer(BigDecimal amount) {
        return status == AccountStatus.ACTIVE
                && availableBalance.compareTo(amount) >= 0
                && dailyTransferredToday.add(amount).compareTo(dailyTransferLimit) <= 0;
    }
}
