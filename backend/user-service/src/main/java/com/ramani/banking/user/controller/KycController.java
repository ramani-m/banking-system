package com.ramani.banking.user.controller;

import com.ramani.banking.user.dto.request.KycUploadRequest;
import com.ramani.banking.user.dto.response.KycDocumentResponse;
import com.ramani.banking.user.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "KYC document submission and status")
public class KycController {

    private final KycService kycService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a KYC document",
               description = "Upload a document URL (passport, national ID, etc.) for identity verification")
    public KycDocumentResponse uploadDocument(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody KycUploadRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpRequest.getRemoteAddr();
        return kycService.uploadDocument(UUID.fromString(userId), request, ip);
    }

    @GetMapping("/status")
    @Operation(summary = "List all my submitted KYC documents and their statuses")
    public List<KycDocumentResponse> getMyDocuments(@AuthenticationPrincipal String userId) {
        return kycService.getMyDocuments(UUID.fromString(userId));
    }
}
