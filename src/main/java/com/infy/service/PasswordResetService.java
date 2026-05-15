package com.infy.service;

import com.infy.dto.ChangePasswordRequestDTO;
import com.infy.dto.ForgotPasswordRequestDTO;
import com.infy.dto.ResetPasswordRequestDTO;
import com.infy.exception.WellnessTrackerException;

public interface PasswordResetService {

    // Step 1 — generate OTP and email it.
    // Always returns silently if the email does not exist (prevents enumeration).
    public void forgotPassword(ForgotPasswordRequestDTO requestDTO);

    // Step 2 — validate OTP and update password.
    public void resetPassword(ResetPasswordRequestDTO requestDTO) throws WellnessTrackerException;
    
    // Change password — email derived from JWT subject, not request body.
    // Prevents any authenticated user from targeting another user's account.
    public void changePassword(String email, ChangePasswordRequestDTO requestDTO) throws WellnessTrackerException;
}
