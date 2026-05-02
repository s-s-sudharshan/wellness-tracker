package com.infy.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.dto.MoodCorrelationDTO;
import com.infy.dto.MoodLogRequestDTO;
import com.infy.dto.MoodLogResponseDTO;
import com.infy.entity.MoodLog;
import com.infy.entity.User;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.MoodLogRepository;
import com.infy.repository.UserRepository;

@Service
@Transactional
public class MoodLogServiceImpl implements MoodLogService {

    @Autowired
    private MoodLogRepository moodLogRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    // Creates a new mood log or updates today's entry if one already exists
    @Override
    public Integer saveMoodLog(MoodLogRequestDTO requestDTO) throws WellnessTrackerException {
        Optional<User> optional = userRepository.findById(requestDTO.getUserId());
        User user = optional.orElseThrow(
                () -> new WellnessTrackerException("Service.USER_NOT_FOUND"));

        Optional<MoodLog> existing = moodLogRepository
                .findByUser_UserIdAndLogDate(requestDTO.getUserId(), requestDTO.getLogDate());

        MoodLog moodLog = existing.orElse(new MoodLog());
        moodLog.setUser(user);
        moodLog.setLogDate(requestDTO.getLogDate());
        moodLog.setMoodScore(requestDTO.getMoodScore());
        moodLog.setNote(requestDTO.getNote());

        return moodLogRepository.save(moodLog).getMoodLogId();
    }

    // US 12 - Mood trend for chart rendering.
    // Returns [] when no mood data exists for the date range — not an error.
    // Consistent with session feedback rule 2d: chart/time-series endpoints return
    // empty list for valid date ranges with no data, never a 400 exception.
    @Override
    @Transactional(readOnly = true)
    public List<MoodLogResponseDTO> getMoodTrend(Integer userId, LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        List<MoodLog> logs = moodLogRepository
                .findByUser_UserIdAndLogDateBetweenOrderByLogDateAsc(userId, fromDate, toDate);

        // Return empty list — the frontend chart renders an empty state without
        // needing special error handling for a routine "no data yet" situation.
        if (logs.isEmpty()) {
            return new ArrayList<>();
        }

        List<MoodLogResponseDTO> responseList = new ArrayList<>();
        for (MoodLog log : logs) {
            MoodLogResponseDTO dto = new MoodLogResponseDTO();
            dto.setMoodLogId(log.getMoodLogId());
            dto.setUserId(log.getUser().getUserId());
            dto.setLogDate(log.getLogDate());
            dto.setMoodScore(log.getMoodScore());
            dto.setMoodLabel(getMoodLabel(log.getMoodScore()));
            dto.setNote(log.getNote());
            dto.setCreatedAt(log.getCreatedAt());
            responseList.add(dto);
        }

        return responseList;
    }

    // US 12 - Mood vs activity correlation for the analytics page.
    // Returns [] when no mood data exists — same reasoning as getMoodTrend above.
    @Override
    @Transactional(readOnly = true)
    public List<MoodCorrelationDTO> getMoodCorrelation(Integer userId, LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException {
        if (!userRepository.existsById(userId)) {
            throw new WellnessTrackerException("Service.USER_NOT_FOUND");
        }

        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        List<MoodLog> moodLogs = moodLogRepository
                .findByUser_UserIdAndLogDateBetweenOrderByLogDateAsc(userId, fromDate, toDate);

        // Return empty list — not an error when the user has not logged mood yet.
        if (moodLogs.isEmpty()) {
            return new ArrayList<>();
        }

        // Build a map of date -> activity count for the same period
        List<Object[]> activityCounts = activityLogRepository
                .countActivitiesPerDayByUserAndDateRange(userId, fromDate, toDate);

        Map<LocalDate, Integer> activityCountByDate = new HashMap<>();
        for (Object[] row : activityCounts) {
            LocalDate date = (LocalDate) row[0];
            Integer count = ((Number) row[1]).intValue();
            activityCountByDate.put(date, count);
        }

        List<MoodCorrelationDTO> result = new ArrayList<>();
        for (MoodLog log : moodLogs) {
            MoodCorrelationDTO dto = new MoodCorrelationDTO();
            dto.setDate(log.getLogDate());
            dto.setMoodScore(log.getMoodScore());
            dto.setMoodLabel(getMoodLabel(log.getMoodScore()));
            int count = activityCountByDate.getOrDefault(log.getLogDate(), 0);
            dto.setActivityCount(count);
            dto.setHadActivity(count > 0);
            result.add(dto);
        }

        return result;
    }

    // Converts numeric mood score to a readable label for the frontend
    private String getMoodLabel(Integer score) {
        return switch (score) {
            case 1 -> "Very Low";
            case 2 -> "Low";
            case 3 -> "OK";
            case 4 -> "Good";
            case 5 -> "Great";
            default -> "Unknown";
        };
    }
}
