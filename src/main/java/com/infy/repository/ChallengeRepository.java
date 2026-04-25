package com.infy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.Challenge;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.VisibilityType;

public interface ChallengeRepository extends CrudRepository<Challenge, Integer> {

    // US 13 - All challenges created by a specific manager
    List<Challenge> findByCreatedBy_UserIdOrderByCreatedAtDesc(Integer userId);

    // US 03 - All ACTIVE or UPCOMING challenges visible company-wide
    List<Challenge> findByStatusInAndVisibilityTypeOrderByStartDateAsc(
            List<ChallengeStatus> statuses, VisibilityType visibilityType);
    
    // US 03 - All ACTIVE or UPCOMING challenges for a specific department
    @Query("SELECT c FROM Challenge c " +
           "WHERE c.status IN :statuses " +
           "AND (c.visibilityType = 'COMPANY_WIDE' " +
           "OR (c.visibilityType = 'DEPARTMENT' AND c.department.departmentId = :deptId)) " +
           "ORDER BY c.startDate ASC")
    List<Challenge> findVisibleChallengesForDepartment(
            @Param("statuses") List<ChallengeStatus> statuses,
            @Param("deptId") Integer deptId);
    
}
