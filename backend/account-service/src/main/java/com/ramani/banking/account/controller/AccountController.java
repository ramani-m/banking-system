package com.ramani.banking.account.controller;

import com.ramani.banking.account.dto.request.CreateAccountRequest;
import com.ramani.banking.account.dto.response.AccountResponse;
import com.ramani.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Account management")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new account")
    public AccountResponse createAccount(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        return accountService.createAccount(UUID.fromString(userId), request);
    }

    @GetMapping
    @Operation(summary = "List all accounts for the current user")
    public List<AccountResponse> getMyAccounts(@AuthenticationPrincipal String userId) {
        return accountService.getUserAccounts(UUID.fromString(userId));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account details")
    public AccountResponse getAccount(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID accountId) {
        return accountService.getAccount(accountId, UUID.fromString(userId));
    }

    @PutMapping("/{accountId}/freeze")
    @Operation(summary = "Freeze an account")
    public AccountResponse freezeAccount(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID accountId) {
        return accountService.freezeAccount(accountId, UUID.fromString(userId));
    }

    @PutMapping("/{accountId}/unfreeze")
    @Operation(summary = "Unfreeze an account (admin only)")
    public AccountResponse unfreezeAccount(@PathVariable UUID accountId) {
        return accountService.unfreezeAccount(accountId);
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close an account")
    public void closeAccount(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID accountId) {
        accountService.closeAccount(accountId, UUID.fromString(userId));
    }
}
