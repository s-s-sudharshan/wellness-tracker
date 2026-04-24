package com.infy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
	@NotBlank(message = "{login.email.absent}")
	@Email(message = "{login.email.invalid}")
	private String email;
 
	@NotBlank(message = "{login.password.absent}")
	private String password;
}
