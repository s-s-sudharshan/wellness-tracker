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
    
    // US 07 - Featured challenges visible to a user's department, non-expired
    // Same visibility rules as the catalog query — COMPANY_WIDE or same-department DEPARTMENT
    // isFeatured = true and endDate >= today ensure only relevant, live featured challenges appear
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.isFeatured = true " +
           "AND c.endDate >= :today " +
           "AND (c.visibilityType = 'COMPANY_WIDE' " +
           "OR (c.visibilityType = 'DEPARTMENT' " +
           "    AND c.department.departmentId = :deptId)) " +
           "ORDER BY c.startDate ASC")
    List<Challenge> findFeaturedChallengesForDepartment(
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
    
    // US 10 - Challenges that started today and are now ACTIVE.
    // Used after bulk sync to identify which challenges just became active
    // so participants can be notified. startDate = today is a narrow window —
    // each challenge only qualifies here on its first day, which combined with
    // the exists duplicate guard in NotificationRepository prevents repeat notifications.
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.startDate = :today " +
           "AND c.status = 'ACTIVE'")
    List<Challenge> findActivatedToday(@Param("today") LocalDate today);
}
