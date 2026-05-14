package com.infy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "{forgotpassword.email.absent}")
    @Email(message = "{forgotpassword.email.invalid}")
    private String email;
}
