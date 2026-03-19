package com.ramani.banking.transaction.service;

import com.ramani.banking.transaction.dto.request.TransferRequest;
import com.ramani.banking.transaction.dto.response.TransactionResponse;
import com.ramani.banking.transaction.entity.*;
import com.ramani.banking.transaction.exception.TransactionException;
import com.ramani.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final String IDEMPOTENCY_PREFIX = "txn:idempotency:";
    private static final long IDEMPOTENCY_TTL_MINUTES = 1440; // 24 hours

    private final TransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestClient accountServiceClient;

    @Transactional
    public TransactionResponse transfer(UUID initiatedBy, TransferRequest request) {
        // Idempotency check — fast path via Redis
        String idempotencyRedisKey = IDEMPOTENCY_PREFIX + request.getIdempotencyKey();
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyRedisKey, request.getIdempotencyKey(),
                        IDEMPOTENCY_TTL_MINUTES, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isNew)) {
            // Already exists — return existing transaction
            return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::toResponse)
                    .orElseThrow(() -> new TransactionException("Idempotency collision — try again"));
        }

        // Validate same-account transfer
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new TransactionException("Cannot transfer to the same account");
        }

        // Create transaction record in PENDING state
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .initiatedBy(initiatedBy)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType() != null ? request.getType() : TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .referenceNumber(generateReference())
                .build();

        transaction = transactionRepository.save(transaction);

        // Execute the actual debit/credit via account service
        try {
            transaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);

            boolean debited = callDebit(request.getFromAccountId(), request.getAmount());
            if (!debited) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Insufficient funds or daily limit exceeded");
                transactionRepository.save(transaction);
                publishTransactionEvent(transaction);
                return toResponse(transaction);
            }

            callCredit(request.getToAccountId(), request.getAmount());

            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("Transaction completed: {} amount: {}", transaction.getId(), transaction.getAmount());
            publishTransactionEvent(transaction);

        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Processing error: " + e.getMessage());
            transactionRepository.save(transaction);
            publishTransactionEvent(transaction);
        }

        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse reverse(UUID transactionId, UUID requestedBy) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found"));

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionException("Only completed transactions can be reversed");
        }
        if (!original.getInitiatedBy().equals(requestedBy)) {
            throw new TransactionException("Unauthorized reversal");
        }

        // Reverse: credit the original source, debit the original destination
        callCredit(original.getFromAccountId(), original.getAmount());
        callDebit(original.getToAccountId(), original.getAmount());

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        Transaction reversal = Transaction.builder()
                .idempotencyKey("reversal:" + original.getId())
                .fromAccountId(original.getToAccountId())
                .toAccountId(original.getFromAccountId())
                .initiatedBy(requestedBy)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .type(TransactionType.REVERSAL)
                .status(TransactionStatus.COMPLETED)
                .referenceNumber(generateReference())
                .description("Reversal of " + original.getReferenceNumber())
                .processedAt(LocalDateTime.now())
                .metadata(Map.of("originalTransactionId", original.getId().toString()))
                .build();

        reversal = transactionRepository.save(reversal);
        log.info("Transaction reversed: {} -> reversal: {}", transactionId, reversal.getId());
        publishTransactionEvent(reversal);

        return toResponse(reversal);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(
            UUID accountId, TransactionStatus status,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, status, from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + id));
    }

    private boolean callDebit(UUID accountId, java.math.BigDecimal amount) {
        try {
            Boolean result = accountServiceClient.post()
                    .uri("/api/v1/internal/accounts/{id}/debit", accountId)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Debit call failed for account {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    private void callCredit(UUID accountId, java.math.BigDecimal amount) {
        accountServiceClient.post()
                .uri("/api/v1/internal/accounts/{id}/credit", accountId)
                .body(Map.of("amount", amount))
                .retrieve()
                .toBodilessEntity();
    }

    private void publishTransactionEvent(Transaction transaction) {
        try {
            kafkaTemplate.send("transaction-events", transaction.getId().toString(),
                    Map.of(
                            "transactionId", transaction.getId(),
                            "status", transaction.getStatus(),
                            "amount", transaction.getAmount(),
                            "fromAccountId", transaction.getFromAccountId(),
                            "toAccountId", transaction.getToAccountId(),
                            "initiatedBy", transaction.getInitiatedBy()
                    ));
        } catch (Exception e) {
            log.warn("Failed to publish transaction event: {}", e.getMessage());
        }
    }

    private String generateReference() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private TransactionResponse toResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .idempotencyKey(txn.getIdempotencyKey())
                .fromAccountId(txn.getFromAccountId())
                .toAccountId(txn.getToAccountId())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .type(txn.getType().name())
                .status(txn.getStatus().name())
                .description(txn.getDescription())
                .referenceNumber(txn.getReferenceNumber())
                .failureReason(txn.getFailureReason())
                .metadata(txn.getMetadata())
                .processedAt(txn.getProcessedAt())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
