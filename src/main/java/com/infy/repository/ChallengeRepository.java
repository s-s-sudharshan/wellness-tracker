package com.infy.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.Challenge;
import com.infy.enums.ChallengeStatus;

public interface ChallengeRepository extends CrudRepository<Challenge, Integer> {

    // US 13 - all challenges created by a specific manager
    List<Challenge> findByCreatedBy_UserIdOrderByCreatedAtDesc(Integer userId);

    // US 03 - active/upcoming challenges visible to employees
    List<Challenge> findByStatusOrderByStartDateAsc(ChallengeStatus status);
}
