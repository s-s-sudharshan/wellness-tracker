package com.infy.repository;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.Department;

public interface DepartmentRepository extends CrudRepository<Department, Integer> {
}