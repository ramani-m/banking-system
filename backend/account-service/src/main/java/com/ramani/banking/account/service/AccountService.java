package com.ramani.banking.account.service;

import com.ramani.banking.account.dto.request.CreateAccountRequest;
import com.ramani.banking.account.dto.response.AccountResponse;
import com.ramani.banking.account.entity.Account;
import com.ramani.banking.account.entity.AccountStatus;
import com.ramani.banking.account.entity.AccountType;
import com.ramani.banking.account.exception.AccountException;
import com.ramani.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        Account account = Account.builder()
                .userId(userId)
                .accountNumber(generateAccountNumber())
                .type(request.getType())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .status(AccountStatus.ACTIVE)
                .dailyLimitResetAt(LocalDateTime.now())
                .build();

        account = accountRepository.save(account);
        log.info("Account created: {} for user: {}", account.getAccountNumber(), userId);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(UUID userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId, UUID requestingUserId) {
        Account account = findAccount(accountId);
        if (!account.getUserId().equals(requestingUserId)) {
            throw new AccountException("Access denied to account");
        }
        return toResponse(account);
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId, UUID requestingUserId) {
        Account account = findAccount(accountId);
        if (!account.getUserId().equals(requestingUserId)) {
            throw new AccountException("Access denied");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountException("Cannot freeze a closed account");
        }
        account.setStatus(AccountStatus.FROZEN);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {
        Account account = findAccount(accountId);
        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new AccountException("Account is not frozen");
        }
        account.setStatus(AccountStatus.ACTIVE);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void closeAccount(UUID accountId, UUID requestingUserId) {
        Account account = findAccount(accountId);
        if (!account.getUserId().equals(requestingUserId)) {
            throw new AccountException("Access denied");
        }
        if (account.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new AccountException("Cannot close account with remaining balance");
        }
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account closed: {}", accountId);
    }

    @Transactional
    public boolean debit(UUID accountId, BigDecimal amount) {
        int updated = accountRepository.debitBalance(accountId, amount);
        if (updated == 0) {
            log.warn("Debit failed for account {} amount {}", accountId, amount);
            return false;
        }
        return true;
    }

    @Transactional
    public void credit(UUID accountId, BigDecimal amount) {
        int updated = accountRepository.creditBalance(accountId, amount);
        if (updated == 0) {
            throw new AccountException("Credit failed for account: " + accountId);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyLimits() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        accountRepository.resetDailyLimits(cutoff);
        log.info("Daily transfer limits reset");
    }

    private Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.valueOf(1_000_000_000L + ThreadLocalRandom.current().nextLong(9_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .type(account.getType().name())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .availableBalance(account.getAvailableBalance())
                .pendingBalance(account.getPendingBalance())
                .totalBalance(account.getTotalBalance())
                .dailyTransferLimit(account.getDailyTransferLimit())
                .dailyTransferredToday(account.getDailyTransferredToday())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
