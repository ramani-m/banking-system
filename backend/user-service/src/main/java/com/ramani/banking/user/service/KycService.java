package com.ramani.banking.user.service;

import com.ramani.banking.user.dto.request.KycUploadRequest;
import com.ramani.banking.user.dto.request.ReviewKycRequest;
import com.ramani.banking.user.dto.response.KycDocumentResponse;
import com.ramani.banking.user.entity.KycDocument;
import com.ramani.banking.user.entity.KycStatus;
import com.ramani.banking.user.exception.UserException;
import com.ramani.banking.user.repository.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public KycDocumentResponse uploadDocument(UUID userId, KycUploadRequest request, String ipAddress) {
        KycDocument doc = KycDocument.builder()
                .userId(userId)
                .type(request.getDocumentType())
                .fileUrl(request.getFileUrl())
                .status(KycStatus.PENDING)
                .build();

        doc = kycDocumentRepository.save(doc);
        log.info("KYC document uploaded: type={} userId={}", request.getDocumentType(), userId);

        auditLogService.log(userId, "KYC_DOCUMENT_UPLOADED", "KYC_DOCUMENT", doc.getId(),
                ipAddress, Map.of("type", request.getDocumentType().name()));

        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getMyDocuments(UUID userId) {
        return kycDocumentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentRepository.findByStatus(KycStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public KycDocumentResponse reviewDocument(UUID documentId, UUID reviewerId,
                                               ReviewKycRequest request, String ipAddress) {
        KycDocument doc = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new UserException("KYC document not found: " + documentId));

        if (doc.getStatus() == KycStatus.APPROVED || doc.getStatus() == KycStatus.REJECTED) {
            throw new UserException("Document has already been reviewed");
        }

        if (request.getStatus() == KycStatus.REJECTED && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new UserException("Rejection reason is required when rejecting a document");
        }

        doc.setStatus(request.getStatus());
        doc.setReviewedBy(reviewerId);
        doc.setReviewedAt(LocalDateTime.now());
        doc.setRejectionReason(request.getRejectionReason());

        doc = kycDocumentRepository.save(doc);
        log.info("KYC document reviewed: id={} status={} by={}", documentId, request.getStatus(), reviewerId);

        auditLogService.log(reviewerId, "KYC_DOCUMENT_REVIEWED", "KYC_DOCUMENT", documentId,
                ipAddress, Map.of("status", request.getStatus().name(), "targetUserId", doc.getUserId().toString()));

        return toResponse(doc);
    }

    private KycDocumentResponse toResponse(KycDocument doc) {
        return KycDocumentResponse.builder()
                .id(doc.getId())
                .documentType(doc.getType().name())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus().name())
                .rejectionReason(doc.getRejectionReason())
                .submittedAt(doc.getCreatedAt())
                .reviewedAt(doc.getReviewedAt())
                .build();
    }
}
