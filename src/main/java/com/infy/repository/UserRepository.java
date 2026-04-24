package com.infy.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.User;

public interface UserRepository extends CrudRepository<User, Integer>{
	Optional<User> findByEmail(String email);
}
