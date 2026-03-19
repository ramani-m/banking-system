package com.ramani.banking.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "MFA code must be 6 digits")
    private String code;
}
