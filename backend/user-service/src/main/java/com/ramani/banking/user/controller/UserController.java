package com.ramani.banking.user.controller;

import com.ramani.banking.user.dto.request.UpdateProfileRequest;
import com.ramani.banking.user.dto.response.UserProfileResponse;
import com.ramani.banking.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Manage your profile")
public class UserController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal String userId) {
        return userProfileService.getProfile(UUID.fromString(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile")
    public UserProfileResponse updateMyProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpRequest.getRemoteAddr();
        return userProfileService.updateProfile(UUID.fromString(userId), request, ip);
    }
}
