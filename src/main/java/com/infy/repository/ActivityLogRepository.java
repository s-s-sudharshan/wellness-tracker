package com.infy.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.ActivityLog;
import com.infy.enums.ActivityType;

public interface ActivityLogRepository extends CrudRepository<ActivityLog, Integer> {

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

    // US 02 - Day-wise trend points for a user, optionally filtered by activity type.
    @Query("SELECT a.activityDate, a.activityType, SUM(a.activityValue), a.unit " +
           "FROM ActivityLog a " +
           "WHERE a.user.userId = :userId " +
           "AND a.activityDate BETWEEN :fromDate AND :toDate " +
           "GROUP BY a.activityDate, a.activityType, a.unit " +
           "ORDER BY a.activityDate ASC")
    List<Object[]> findTrendByUserAndDateRange(
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

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

    // US 01 - ownership check for edit and delete
    // Returns the log only if it belongs to the given user — enforces ownership at DB level
    Optional<ActivityLog> findByActivityLogIdAndUser_UserId(
            Integer activityLogId, Integer userId);
    
    // US 05 
    // All-time SUM for ALL activity types in one query — replaces 5 individual calls
    // Returns rows of [ActivityType, Double].
    // Only types the user has actually logged appear — missing types default to 0.0 in service.
    @Query("SELECT a.activityType, COALESCE(SUM(a.activityValue), 0.0) " +
           "FROM ActivityLog a " +
           "WHERE a.user.userId = :userId " +
           "GROUP BY a.activityType")
    List<Object[]> getAllTimeTotalsByUser(@Param("userId") Integer userId);
    
    // Best single-day total for a given type — used by DAILY_* badges
    // Native query because JPQL does not support derived tables in FROM
    @Query(value =
           "SELECT COALESCE(MAX(daily.dayTotal), 0.0) FROM " +
           "(SELECT SUM(activity_value) AS dayTotal " +
           " FROM activity_logs " +
           " WHERE user_id = :userId AND activity_type = :type " +
           " GROUP BY activity_date) daily",
           nativeQuery = true)
    Double getDailyBestByType(
            @Param("userId") Integer userId,
            @Param("type") String type);   // String — nativeQuery can't bind enum directly
    
    // Best single-week total for a given type — used by WEEKLY_* badges
    // WEEK() groups by calendar week; outer query picks the max weekly total
    @Query(value =
           "SELECT COALESCE(MAX(weekly.weekTotal), 0.0) FROM " +
           "(SELECT SUM(activity_value) AS weekTotal " +
           " FROM activity_logs " +
           " WHERE user_id = :userId AND activity_type = :type " +
           " GROUP BY YEAR(activity_date), WEEK(activity_date)) weekly",
           nativeQuery = true)
    Double getWeeklyBestByType(
            @Param("userId") Integer userId,
            @Param("type") String type);   // String — nativeQuery can't bind enum directly
    
    // Recent distinct active dates — used for streak calculation.
    // Pageable limits rows to avoid loading full history for long-term users.
    // 400 rows covers a year-plus streak — more than any realistic streak length.
    @Query("SELECT DISTINCT a.activityDate FROM ActivityLog a " +
           "WHERE a.user.userId = :userId ORDER BY a.activityDate DESC")
    List<LocalDate> getActiveDates(@Param("userId") Integer userId, Pageable pageable);
 
    // Total number of activity log entries ever — used by TOTAL_LOGS badge
    Integer countByUser_UserId(Integer userId);
 
    // Count of distinct activity types ever logged — used by ACTIVITY_VARIETY badge
    @Query("SELECT COUNT(DISTINCT a.activityType) FROM ActivityLog a " +
           "WHERE a.user.userId = :userId")
    Integer countDistinctActivityTypes(@Param("userId") Integer userId);
    
}
