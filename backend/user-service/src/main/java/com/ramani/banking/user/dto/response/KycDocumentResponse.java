package com.ramani.banking.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class KycDocumentResponse {
    private UUID id;
    private String documentType;
    private String fileUrl;
    private String status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
