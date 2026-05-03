package com.infy.repository;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.Badge;

public interface BadgeRepository extends CrudRepository<Badge, Integer> {
    // CrudRepository.findAll() is sufficient — no custom queries needed
}
