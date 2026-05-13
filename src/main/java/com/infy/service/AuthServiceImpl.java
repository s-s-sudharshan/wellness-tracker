package com.infy.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.LoginRequestDTO;
import com.infy.dto.LoginResponseDTO;
import com.infy.entity.User;
import com.infy.enums.UserStatus;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.UserRepository;
import com.infy.security.JwtService;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Override
    public LoginResponseDTO login(LoginRequestDTO requestDTO) throws WellnessTrackerException {

        Optional<User> optional = userRepository.findByEmail(requestDTO.getEmail());
        User user = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.INVALID_CREDENTIALS"));

        // BCrypt comparison — replaces the previous plain-text equals() check
        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPasswordHash())) {
            throw new WellnessTrackerException("Service.INVALID_CREDENTIALS");
        }

        // Separate error for inactive accounts — lets the user know to contact HR
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new WellnessTrackerException("Service.ACCOUNT_INACTIVE");
        }

        // Generate JWT — subject is the user's email
        String token = jwtService.generateToken(user.getEmail());

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setUserId(user.getUserId());
        responseDTO.setFirstName(user.getFirstName());
        responseDTO.setLastName(user.getLastName());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setRole(user.getRole());
        responseDTO.setDepartmentId(user.getDepartment().getDepartmentId());
        responseDTO.setDepartmentName(user.getDepartment().getDepartmentName());
        responseDTO.setToken(token);

        return responseDTO;
    }
}
