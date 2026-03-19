package com.ramani.banking.user.dto.request;

import com.ramani.banking.user.entity.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewKycRequest {

    @NotNull(message = "Status is required")
    private KycStatus status;

    private String rejectionReason;
}
