package com.ramani.banking.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String qrCodeImage;
}
