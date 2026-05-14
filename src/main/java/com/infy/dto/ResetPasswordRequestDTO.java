package com.infy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "{resetpassword.email.absent}")
    @Email(message = "{resetpassword.email.invalid}")
    private String email;

    @NotBlank(message = "{resetpassword.otp.absent}")
    @Pattern(regexp = "\\d{6}", message = "{resetpassword.otp.invalid}")
    private String otp;

    @NotBlank(message = "{resetpassword.newpassword.absent}")
    @Size(min = 8, message = "{resetpassword.newpassword.tooshort}")
    private String newPassword;
}
