package com.infy.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.LoginRequestDTO;
import com.infy.dto.LoginResponseDTO;
import com.infy.entity.User;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.UserRepository;


@Service
@Transactional
public class AuthServiceImpl implements AuthService {

	@Autowired
	private UserRepository userRepository;
	
	@Override
	public LoginResponseDTO login(LoginRequestDTO requestDTO) throws WellnessTrackerException {
		Optional<User> optional = userRepository.findByEmail(requestDTO.getEmail());
		User user = optional.orElseThrow(() -> new WellnessTrackerException("Service.INVALID_CREDENTIALS"));

		// NOTE: Plain text comparison used temporarily until Spring Security is implemented.
		// When Spring Security is added, replace this line with:
		// if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPasswordHash()))
		if (!requestDTO.getPassword().equals(user.getPasswordHash())) {
			throw new WellnessTrackerException("Service.INVALID_CREDENTIALS");
		}

		if (user.getStatus() != com.infy.enums.UserStatus.ACTIVE) {
			throw new WellnessTrackerException("Service.ACCOUNT_INACTIVE");
		}

		LoginResponseDTO responseDTO = new LoginResponseDTO();
		responseDTO.setUserId(user.getUserId());
		responseDTO.setFirstName(user.getFirstName());
		responseDTO.setLastName(user.getLastName());
		responseDTO.setEmail(user.getEmail());
		responseDTO.setRole(user.getRole());
		responseDTO.setDepartmentId(user.getDepartment().getDepartmentId());
		responseDTO.setDepartmentName(user.getDepartment().getDepartmentName());

		return responseDTO;
	}

}
