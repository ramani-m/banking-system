package com.ramani.banking.user.dto.request;

import com.ramani.banking.user.entity.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycUploadRequest {

    @NotNull(message = "Document type is required")
    private KycDocumentType documentType;

    @NotBlank(message = "File URL is required")
    private String fileUrl;
}
