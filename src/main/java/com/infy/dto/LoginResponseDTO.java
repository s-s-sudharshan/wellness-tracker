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
}
