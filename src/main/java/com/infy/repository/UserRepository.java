package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.User;
import com.infy.enums.UserStatus;

public interface UserRepository extends CrudRepository<User, Integer>{
	
	Optional<User> findByEmail(String email);
	
    // US 10 - Notify active users in a specific department (DEPARTMENT visibility challenges)
    List<User> findByDepartment_DepartmentIdAndStatus(Integer departmentId, UserStatus status);

    // US 10 - Notify all active users (COMPANY_WIDE visibility challenges)
    List<User> findByStatus(UserStatus status);
}
