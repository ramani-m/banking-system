package com.ramani.banking.user.controller;

import com.ramani.banking.user.dto.request.ReviewKycRequest;
import com.ramani.banking.user.dto.response.KycDocumentResponse;
import com.ramani.banking.user.dto.response.UserProfileResponse;
import com.ramani.banking.user.service.KycService;
import com.ramani.banking.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Users", description = "Admin-only user and KYC management")
public class AdminUserController {

    private final UserProfileService userProfileService;
    private final KycService kycService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all user profiles (admin only)")
    public List<UserProfileResponse> getAllUsers() {
        return userProfileService.getAllUsers();
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all pending KYC documents for review (admin only)")
    public List<KycDocumentResponse> getPendingKyc() {
        return kycService.getPendingDocuments();
    }

    @PutMapping("/kyc/{documentId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve or reject a KYC document (admin only)")
    public KycDocumentResponse reviewKycDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal String reviewerId,
            @Valid @RequestBody ReviewKycRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpRequest.getRemoteAddr();
        return kycService.reviewDocument(documentId, UUID.fromString(reviewerId), request, ip);
    }
}
