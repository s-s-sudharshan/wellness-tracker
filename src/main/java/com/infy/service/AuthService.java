package com.infy.service;

import com.infy.dto.LoginRequestDTO;
import com.infy.dto.LoginResponseDTO;
import com.infy.exception.WellnessTrackerException;

public interface AuthService {
	public LoginResponseDTO login(LoginRequestDTO requestDTO) throws WellnessTrackerException;
	
}
