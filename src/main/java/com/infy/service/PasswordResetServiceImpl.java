package com.infy.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.ForgotPasswordRequestDTO;
import com.infy.dto.ResetPasswordRequestDTO;
import com.infy.entity.PasswordResetOtp;
import com.infy.entity.User;
import com.infy.enums.UserStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.PasswordResetOtpRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Log LOGGER = LogFactory.getLog(PasswordResetServiceImpl.class);

    // SecureRandom is thread-safe and cryptographically strong — required for OTPs
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.password-reset.otp.expiry.minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.password-reset.otp.max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${spring.mail.username}")
    private String fromAddress;

    // Step 1 — generate OTP, store hashed, send email.
    // If the email does not exist or the user is inactive, return silently.
    // This prevents account enumeration — caller always receives the same generic message.
    @Override
    public void forgotPassword(ForgotPasswordRequestDTO requestDTO) {
        Optional<User> userOptional = userRepository.findByEmail(requestDTO.getEmail());

        if (userOptional.isEmpty()
                || !UserStatus.ACTIVE.equals(userOptional.get().getStatus())) {
            // Silently return — do not reveal whether account exists or is inactive
            LOGGER.info("PasswordResetServiceImpl.forgotPassword: no-op for email="
                    + requestDTO.getEmail());
            return;
        }

        User user = userOptional.get();

        // Invalidate all previous unused OTPs for this user before issuing a new one
        passwordResetOtpRepository.invalidateAllForUser(user.getUserId());

        // Generate a cryptographically secure 6-digit OTP and hash it for storage
        String rawOtp    = generateSixDigitOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        PasswordResetOtp otpRecord = new PasswordResetOtp();
        otpRecord.setUser(user);
        otpRecord.setOtpHash(hashedOtp);
        otpRecord.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        passwordResetOtpRepository.save(otpRecord);

        // Fire-and-forget email — log failure clearly, do not surface to caller.
        // The API always returns the generic success message regardless.
        // Do NOT log the raw OTP — keep it out of production logs.
        try {
            sendOtpEmail(user.getEmail(), rawOtp);
        } catch (Exception e) {
            LOGGER.error("PasswordResetServiceImpl.forgotPassword: failed to send OTP email"
                    + " to userId=" + user.getUserId() + " — " + e.getMessage(), e);
        }
    }

    // Step 2 — validate OTP, enforce attempt limits, update password.
    // Uses Service.INVALID_RESET_OTP for all failure cases (unknown email,
    // inactive user, expired/used/exhausted OTP, wrong OTP) to avoid leaking
    // account status or existence through the reset flow.
    @Override
    public void resetPassword(ResetPasswordRequestDTO requestDTO)
            throws WellnessTrackerException {

        // Look up user by email — use INVALID_RESET_OTP (not USER_NOT_FOUND)
        // to avoid revealing whether the email is registered
        Optional<User> userOptional = userRepository.findByEmail(requestDTO.getEmail());
        if (userOptional.isEmpty()) {
            throw new WellnessTrackerException("Service.INVALID_RESET_OTP");
        }

        User user = userOptional.get();

        // Re-check active status — user may have been deactivated after OTP was issued.
        // Use INVALID_RESET_OTP (not ACCOUNT_INACTIVE) to avoid leaking account status.
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new WellnessTrackerException("Service.INVALID_RESET_OTP");
        }

        // Fetch all active (unused, non-expired) OTPs for this user
        List<PasswordResetOtp> activeOtps =
                passwordResetOtpRepository.findActiveByUserId(user.getUserId());

        if (activeOtps.isEmpty()) {
            throw new WellnessTrackerException("Service.INVALID_RESET_OTP");
        }

        // Most recent active OTP is at index 0 (ordered by createdAt DESC)
        PasswordResetOtp otpRecord = activeOtps.get(0);

        // Block if attempt count has already reached the maximum
        if (otpRecord.getAttemptCount() >= otpMaxAttempts) {
            throw new WellnessTrackerException("Service.INVALID_RESET_OTP");
        }

        // Verify the raw OTP against the stored BCrypt hash
        boolean matches = passwordEncoder.matches(requestDTO.getOtp(), otpRecord.getOtpHash());

        if (!matches) {
            // Increment attempt counter — invalidate immediately if max reached
            otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);
            if (otpRecord.getAttemptCount() >= otpMaxAttempts) {
                otpRecord.setUsed(true);
                LOGGER.info("PasswordResetServiceImpl.resetPassword: OTP exhausted"
                        + " for userId=" + user.getUserId());
            }
            passwordResetOtpRepository.save(otpRecord);
            throw new WellnessTrackerException("Service.INVALID_RESET_OTP");
        }

        // OTP matched — update the password and mark OTP as used
        user.setPasswordHash(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);

        otpRecord.setUsed(true);
        passwordResetOtpRepository.save(otpRecord);

        LOGGER.info("PasswordResetServiceImpl.resetPassword: password reset successful"
                + " for userId=" + user.getUserId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    // Generates a zero-padded 6-digit OTP string (000000–999999).
    // Uses SecureRandom — cryptographically strong, not predictable like java.util.Random.
    private String generateSixDigitOtp() {
        int raw = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", raw);
    }

    private void sendOtpEmail(String toAddress, String rawOtp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toAddress);
        message.setSubject("Your Wellness Tracker password reset code");
        message.setText(
                "Hello,\n\n" +
                "Your password reset code is: " + rawOtp + "\n\n" +
                "This code is valid for " + otpExpiryMinutes + " minutes.\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "— Wellness Tracker Team"
        );
        mailSender.send(message);
    }
}
