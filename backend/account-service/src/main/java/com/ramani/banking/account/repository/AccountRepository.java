package com.ramani.banking.account.repository;

import com.ramani.banking.account.entity.Account;
import com.ramani.banking.account.entity.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserId(UUID userId);

    List<Account> findByUserIdAndStatus(UUID userId, AccountStatus status);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(UUID id);

    @Modifying
    @Query("UPDATE Account a SET a.availableBalance = a.availableBalance - :amount, " +
           "a.dailyTransferredToday = a.dailyTransferredToday + :amount " +
           "WHERE a.id = :id AND a.availableBalance >= :amount")
    int debitBalance(UUID id, BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.availableBalance = a.availableBalance + :amount WHERE a.id = :id")
    int creditBalance(UUID id, BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.pendingBalance = a.pendingBalance + :amount WHERE a.id = :id")
    void addPendingBalance(UUID id, BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.pendingBalance = a.pendingBalance - :amount, " +
           "a.availableBalance = a.availableBalance + :amount WHERE a.id = :id")
    void settlePendingBalance(UUID id, BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.dailyTransferredToday = 0, a.dailyLimitResetAt = CURRENT_TIMESTAMP " +
           "WHERE a.dailyLimitResetAt < :cutoff")
    void resetDailyLimits(java.time.LocalDateTime cutoff);
}
