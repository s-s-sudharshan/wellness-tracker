package com.infy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.Recommendation;
import com.infy.enums.RecommendationStatus;

public interface RecommendationRepository extends CrudRepository<Recommendation, Integer> {
	
    // US 08 - Fetch all ACTIVE recommendations for a user, newest first
    List<Recommendation> findByUser_UserIdAndStatusOrderByCreatedAtDesc(
            Integer userId, RecommendationStatus status);
    
    // US 08 - Purge existing ACTIVE recommendations before regenerating
    // Called every time the rule engine refreshes so the list stays current
    @Modifying
    @Query("DELETE FROM Recommendation r WHERE r.user.userId = :userId AND r.status = 'ACTIVE'")
    void deleteActiveByUserId(@Param("userId") Integer userId);

}
