package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.ChangePasswordRequestDTO;
import com.infy.dto.ForgotPasswordRequestDTO;
import com.infy.dto.ResetPasswordRequestDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.PasswordResetService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class PasswordResetAPI {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private Environment env;

    // POST /wellness/forgot-password — public endpoint, no JWT required.
    // Always returns the same generic message regardless of whether the email exists.
    // This prevents account enumeration.
    @PostMapping(value = "/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO requestDTO) {
        passwordResetService.forgotPassword(requestDTO);
        String message = env.getProperty("API.FORGOT_PASSWORD_SUCCESS");
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    // POST /wellness/reset-password — public endpoint, no JWT required.
    // Validates OTP, enforces attempt limits, and updates the user's password.
    @PostMapping(value = "/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO requestDTO)
            throws WellnessTrackerException {
        passwordResetService.resetPassword(requestDTO);
        String message = env.getProperty("API.RESET_PASSWORD_SUCCESS");
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    // PUT /wellness/change-password — protected endpoint, JWT required.
    // The authenticated user is derived from the JWT subject in SecurityContext.
    // userId is never accepted from the request body — prevents IDOR where any
    // authenticated user could target another user's account.
    @PutMapping(value = "/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDTO requestDTO)
            throws WellnessTrackerException {
        // userDetails is guaranteed non-null by Spring Security when the endpoint is
        // protected, but we guard defensively in case security config changes later.
        if (userDetails == null) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }
        // userDetails.getUsername() returns the email set as JWT subject at login
        passwordResetService.changePassword(userDetails.getUsername(), requestDTO);
        String message = env.getProperty("API.CHANGE_PASSWORD_SUCCESS");
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
