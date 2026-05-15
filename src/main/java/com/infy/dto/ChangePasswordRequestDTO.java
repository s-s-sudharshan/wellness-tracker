package com.infy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {

    // The user is derived from the JWT subject in SecurityContext — see PasswordResetAPI.

    @NotBlank(message = "{changepassword.currentpassword.absent}")
    private String currentPassword;

    @NotBlank(message = "{changepassword.newpassword.absent}")
    @Size(min = 8, message = "{changepassword.newpassword.tooshort}")
    private String newPassword;
}
