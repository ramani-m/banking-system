package com.ramani.banking.user.service;

import com.ramani.banking.user.dto.request.UpdateProfileRequest;
import com.ramani.banking.user.dto.response.KycDocumentResponse;
import com.ramani.banking.user.dto.response.UserProfileResponse;
import com.ramani.banking.user.entity.KycDocument;
import com.ramani.banking.user.entity.KycStatus;
import com.ramani.banking.user.entity.UserProfile;
import com.ramani.banking.user.repository.KycDocumentRepository;
import com.ramani.banking.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyProfile(userId));
        String kycStatus = resolveKycStatus(userId);
        return toResponse(profile, kycStatus);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request, String ipAddress) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());

        if (request.getFirstName() != null)   profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null)    profile.setLastName(request.getLastName());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddressLine1() != null) profile.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) profile.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null)        profile.setCity(request.getCity());
        if (request.getState() != null)       profile.setState(request.getState());
        if (request.getPostalCode() != null)  profile.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null)     profile.setCountry(request.getCountry());

        profile = profileRepository.save(profile);
        log.info("Profile updated for userId={}", userId);

        auditLogService.log(userId, "PROFILE_UPDATED", "USER_PROFILE", profile.getId(),
                ipAddress, Map.of("fields", request.toString()));

        return toResponse(profile, resolveKycStatus(userId));
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return profileRepository.findAll().stream()
                .map(p -> toResponse(p, resolveKycStatus(p.getUserId())))
                .toList();
    }

    private UserProfile createEmptyProfile(UUID userId) {
        return profileRepository.save(UserProfile.builder().userId(userId).build());
    }

    private String resolveKycStatus(UUID userId) {
        List<KycDocument> docs = kycDocumentRepository.findByUserId(userId);
        if (docs.isEmpty()) return "NOT_SUBMITTED";
        if (docs.stream().anyMatch(d -> d.getStatus() == KycStatus.APPROVED)) return "APPROVED";
        if (docs.stream().anyMatch(d -> d.getStatus() == KycStatus.UNDER_REVIEW)) return "UNDER_REVIEW";
        if (docs.stream().anyMatch(d -> d.getStatus() == KycStatus.PENDING)) return "PENDING";
        return "REJECTED";
    }

    private UserProfileResponse toResponse(UserProfile p, String kycStatus) {
        return UserProfileResponse.builder()
                .userId(p.getUserId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .dateOfBirth(p.getDateOfBirth())
                .addressLine1(p.getAddressLine1())
                .addressLine2(p.getAddressLine2())
                .city(p.getCity())
                .state(p.getState())
                .postalCode(p.getPostalCode())
                .country(p.getCountry())
                .kycStatus(kycStatus)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
