package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.infy.entity.UserBadge;

public interface UserBadgeRepository extends CrudRepository<UserBadge, Integer> {

    // Load all badges earned by a user, newest first — used to build earnedMap
    List<UserBadge> findByUser_UserIdOrderByEarnedAtDesc(Integer userId);

    // Idempotency check before awarding — fast exists query, no entity load
    boolean existsByUser_UserIdAndBadge_BadgeId(Integer userId, Integer badgeId);

    // P1 fix — fetch existing row on race condition path so earnedAt is never null
    Optional<UserBadge> findByUser_UserIdAndBadge_BadgeId(Integer userId, Integer badgeId);
}
