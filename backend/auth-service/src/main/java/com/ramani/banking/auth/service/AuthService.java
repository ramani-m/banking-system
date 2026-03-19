package com.ramani.banking.auth.service;

import com.ramani.banking.auth.dto.request.*;
import com.ramani.banking.auth.dto.response.AuthResponse;
import com.ramani.banking.auth.dto.response.MfaSetupResponse;
import com.ramani.banking.auth.entity.*;
import com.ramani.banking.auth.exception.AuthException;
import com.ramani.banking.auth.exception.UserAlreadyExistsException;
import com.ramani.banking.auth.repository.PasswordResetTokenRepository;
import com.ramani.banking.auth.repository.RefreshTokenRepository;
import com.ramani.banking.auth.repository.UserRepository;
import com.ramani.banking.auth.util.JwtUtil;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final QrGenerator qrGenerator;

    @Value("${app.name:Ramani Banking}")
    private String appName;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshToken refreshToken = createRefreshToken(user, null, null);

        return buildAuthResponse(user, accessToken, refreshToken.getTokenHash());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (user.isLocked()) {
            throw new AuthException("Account is locked. Try again after " + user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("Account is not active. Status: " + user.getStatus());
        }

        if (user.isMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isBlank()) {
                return AuthResponse.builder()
                        .mfaRequired(true)
                        .mfaEnabled(true)
                        .userId(user.getId())
                        .build();
            }
            verifyTotpCode(user, request.getMfaCode());
        }

        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
        userRepository.resetFailedAttempts(user.getId());

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshToken refreshToken = createRefreshToken(user, ipAddress, userAgent);

        log.info("User logged in: {}", user.getId());
        return buildAuthResponse(user, accessToken, refreshToken.getTokenHash());
    }

    @Transactional
    public AuthResponse refreshTokens(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshToken newRefreshToken = createRefreshToken(user, refreshToken.getIpAddress(), refreshToken.getDeviceInfo());

        return buildAuthResponse(user, newAccessToken, newRefreshToken.getTokenHash());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        String tokenHash = hashToken(refreshTokenValue);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("All refresh tokens revoked for user: {}", userId);
    }

    @Transactional
    public MfaSetupResponse setupMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(appName)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(qrData);
            String qrImage = getDataUriForImage(imageData, qrGenerator.getImageMimeType());
            return MfaSetupResponse.builder()
                    .secret(secret)
                    .qrCodeUrl(qrData.getUri())
                    .qrCodeImage(qrImage)
                    .build();
        } catch (Exception e) {
            throw new AuthException("Failed to generate QR code");
        }
    }

    @Transactional
    public void enableMfa(UUID userId, MfaVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        verifyTotpCode(user, request.getCode());
        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("MFA enabled for user: {}", userId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new AuthException("Reset token is expired or already used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllUserTokens(user.getId());
        log.info("Password reset for user: {}", user.getId());
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        passwordResetTokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
        log.info("Expired tokens cleaned up");
    }

    private void handleFailedLogin(User user) {
        userRepository.incrementFailedAttempts(user.getId());
        if (user.getFailedLoginAttempts() + 1 >= MAX_FAILED_ATTEMPTS) {
            userRepository.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked due to failed attempts: {}", user.getId());
        }
    }

    private RefreshToken createRefreshToken(User user, String ipAddress, String deviceInfo) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiryMs() / 1000))
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private void verifyTotpCode(User user, String code) {
        if (user.getMfaSecret() == null) {
            throw new AuthException("MFA not configured");
        }
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, new SystemTimeProvider());
        if (!verifier.isValidCode(user.getMfaSecret(), code)) {
            throw new AuthException("Invalid MFA code");
        }
    }

    private String hashToken(String token) {
        return Base64.getEncoder().encodeToString(
                org.springframework.util.DigestUtils.md5Digest(token.getBytes())
        );
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }
}
