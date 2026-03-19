package com.ramani.banking.transaction.repository;

import com.ramani.banking.transaction.entity.Transaction;
import com.ramani.banking.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId)
        AND (:status IS NULL OR t.status = :status)
        AND (:from IS NULL OR t.createdAt >= :from)
        AND (:to IS NULL OR t.createdAt <= :to)
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findByAccountId(
            UUID accountId,
            TransactionStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.initiatedBy = :userId
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
}
