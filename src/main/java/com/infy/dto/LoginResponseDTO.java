package com.infy.dto;

import com.infy.enums.Role;

import lombok.Data;

@Data
public class LoginResponseDTO {
	private Integer userId;
	private String firstName;
	private String lastName;
	private String email;
	private Role role;
	private Integer departmentId;
	private String departmentName;
	
    // JWT Bearer token — set by AuthServiceImpl after successful authentication.
    // Frontend stores this and sends it as: Authorization: Bearer <token>
    private String token;
}
