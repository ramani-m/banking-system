package com.ramani.banking.transaction.controller;

import com.ramani.banking.transaction.dto.request.TransferRequest;
import com.ramani.banking.transaction.dto.response.TransactionResponse;
import com.ramani.banking.transaction.entity.TransactionStatus;
import com.ramani.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Transaction management")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate a fund transfer")
    public TransactionResponse transfer(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(UUID.fromString(userId), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        return transactionService.getTransaction(id);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transaction history for an account")
    public Page<TransactionResponse> getHistory(
            @PathVariable UUID accountId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return transactionService.getTransactionHistory(accountId, status, from, to, pageable);
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse a completed transaction")
    public TransactionResponse reverse(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID id) {
        return transactionService.reverse(id, UUID.fromString(userId));
    }
}
