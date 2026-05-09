package com.infy.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.infy.dto.LoginRequestDTO;
import com.infy.dto.LoginResponseDTO;
import com.infy.exception.WellnessTrackerException;
import com.infy.service.AuthService;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/wellness")
public class AuthAPI {
	
	@Autowired
	AuthService authService;
	
	@PostMapping(value = "/login")
	public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) 
			throws WellnessTrackerException {
		LoginResponseDTO response = authService.login(requestDTO);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
