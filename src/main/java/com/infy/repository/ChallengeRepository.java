package com.infy.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.Challenge;

public interface ChallengeRepository extends CrudRepository<Challenge, Integer> {

    // US 13 - All challenges created by a specific manager
    List<Challenge> findByCreatedBy_UserIdOrderByCreatedAtDesc(Integer userId);

    // US 03 - All non-expired challenges visible to a user's department
    // Filters by endDate >= today so status column staleness doesn't affect results
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.endDate >= :today " +
           "AND (c.visibilityType = 'COMPANY_WIDE' " +
           "OR (c.visibilityType = 'DEPARTMENT' " +
           "    AND c.department.departmentId = :deptId)) " +
           "ORDER BY c.startDate ASC")
    List<Challenge> findVisibleChallengesForDepartment(
            @Param("today") LocalDate today,
            @Param("deptId") Integer deptId);

    // US 04 - All challenges for leaderboard (no status filter needed)
    // Already covered by CrudRepository.findById()

    // Status sync - bulk update UPCOMING -> ACTIVE when startDate has passed
    @Modifying
    @Query("UPDATE Challenge c SET c.status = 'ACTIVE' " +
           "WHERE c.status = 'UPCOMING' " +
           "AND c.startDate <= :today " +
           "AND c.endDate >= :today")
    int activateStartedChallenges(@Param("today") LocalDate today);

    // Status sync - bulk update ACTIVE/UPCOMING -> COMPLETED when endDate has passed
    @Modifying
    @Query("UPDATE Challenge c SET c.status = 'COMPLETED' " +
           "WHERE c.status != 'COMPLETED' " +
           "AND c.endDate < :today")
    int completeExpiredChallenges(@Param("today") LocalDate today);
}
