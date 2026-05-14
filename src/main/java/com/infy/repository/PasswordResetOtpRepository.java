package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.PasswordResetOtp;

public interface PasswordResetOtpRepository extends CrudRepository<PasswordResetOtp, Integer> {

    // Fetch all unused, non-expired OTPs for a user — used for attempt-count check
    // and to find a valid OTP candidate during reset.
    // Ordered newest-first so the most recently issued OTP is checked first.
    @Query("SELECT o FROM PasswordResetOtp o " +
           "WHERE o.user.userId = :userId " +
           "AND o.used = false " +
           "AND o.expiresAt > CURRENT_TIMESTAMP " +
           "ORDER BY o.createdAt DESC")
    List<PasswordResetOtp> findActiveByUserId(@Param("userId") Integer userId);

    // Fetch the latest OTP (any state) for a user — used to load for attempt tracking
    Optional<PasswordResetOtp> findTopByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    // Invalidate all unused OTPs for a user when a new one is issued
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.used = true " +
           "WHERE o.user.userId = :userId AND o.used = false")
    void invalidateAllForUser(@Param("userId") Integer userId);
}
