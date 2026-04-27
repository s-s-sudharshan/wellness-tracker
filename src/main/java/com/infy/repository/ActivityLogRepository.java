package com.infy.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.ActivityLog;
import com.infy.enums.ActivityType;

public interface ActivityLogRepository extends CrudRepository<ActivityLog, Integer>{
	
	List<ActivityLog> findByUser_UserIdOrderByActivityDateDescCreatedAtDesc(Integer userId);
	
	List<ActivityLog> findByUser_UserIdAndActivityDateBetweenOrderByActivityDateDesc(
			Integer userId, LocalDate fromDate, LocalDate toDate);
	
	@Query("SELECT a.activityType, SUM(a.activityValue), a.unit " + 
		    "FROM ActivityLog a " + 
			"WHERE a.user.userId = :userId " +
		    "AND a.activityDate BETWEEN :fromDate AND :toDate " +
			"GROUP BY a.activityType, a.unit")
	List<Object[]> findSummaryByUserAndDateRange(
			 @Param("userId") Integer userId,
			 @Param("fromDate") LocalDate fromDate,
			 @Param("toDate") LocalDate toDate);
	
	Integer countByUser_UserIdAndActivityDateBetween(
			Integer userId, LocalDate fromDate, LocalDate toDate);
	
	// US 11 - sum actuals per activity type for weekly progress
	@Query("SELECT a.activityType, SUM(a.activityValue) " +
		   "FROM ActivityLog a " +
		   "WHERE a.user.userId = :userId " +
		   "AND a.activityDate BETWEEN :fromDate AND :toDate " +
		   "GROUP BY a.activityType")
	List<Object[]> findActualsByUserAndDateRange(
			@Param("userId") Integer userId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

	// US 12 - count activities per day for mood correlation
	@Query("SELECT a.activityDate, COUNT(a) " +
		   "FROM ActivityLog a " +
		   "WHERE a.user.userId = :userId " +
		   "AND a.activityDate BETWEEN :fromDate AND :toDate " +
		   "GROUP BY a.activityDate")
	List<Object[]> countActivitiesPerDayByUserAndDateRange(
			@Param("userId") Integer userId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);
	
    // US 04 - batch fetch actuals for ALL participants of a challenge in one query
    // Returns: [userId, totalValue] filtered by a specific activityType
    @Query("SELECT a.user.userId, SUM(a.activityValue) " +
           "FROM ActivityLog a " +
           "WHERE a.user.userId IN :userIds " +
           "AND a.activityType = :activityType " +
           "AND a.activityDate BETWEEN :fromDate AND :toDate " +
           "GROUP BY a.user.userId")
    List<Object[]> findActualsByUsersAndDateRangeAndType(
            @Param("userIds") List<Integer> userIds,
            @Param("activityType") ActivityType activityType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
