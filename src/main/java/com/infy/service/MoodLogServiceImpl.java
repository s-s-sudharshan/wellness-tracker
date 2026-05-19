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
import com.infy.security.AuthenticatedUserResolver;

@Service
@Transactional
public class MoodLogServiceImpl implements MoodLogService {

    @Autowired
    private MoodLogRepository moodLogRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    // Creates a new mood log or updates today's entry if one already exists.
    // Caller identity derived from JWT — never from request body.
    @Override
    public Integer saveMoodLog(MoodLogRequestDTO requestDTO) throws WellnessTrackerException {
        User caller = authenticatedUserResolver.resolveCurrentUser();

        Optional<MoodLog> existing = moodLogRepository
                .findByUser_UserIdAndLogDate(caller.getUserId(), requestDTO.getLogDate());

        MoodLog moodLog = existing.orElse(new MoodLog());
        moodLog.setUser(caller);
        moodLog.setLogDate(requestDTO.getLogDate());
        moodLog.setMoodScore(requestDTO.getMoodScore());
        moodLog.setNote(requestDTO.getNote());

        return moodLogRepository.save(moodLog).getMoodLogId();
    }

    // US 12 - Mood trend for chart rendering.
    // Returns [] when no mood data exists for the date range — not an error.
    @Override
    @Transactional(readOnly = true)
    public List<MoodLogResponseDTO> getMoodTrend(LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        List<MoodLog> logs = moodLogRepository
                .findByUser_UserIdAndLogDateBetweenOrderByLogDateAsc(callerId, fromDate, toDate);

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
    public List<MoodCorrelationDTO> getMoodCorrelation(LocalDate fromDate,
            LocalDate toDate) throws WellnessTrackerException {
        Integer callerId = authenticatedUserResolver.resolveCurrentUserId();

        if (fromDate.isAfter(toDate)) {
            throw new WellnessTrackerException("Service.INVALID_DATE_RANGE");
        }

        List<MoodLog> moodLogs = moodLogRepository
                .findByUser_UserIdAndLogDateBetweenOrderByLogDateAsc(callerId, fromDate, toDate);

        if (moodLogs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object[]> activityCounts = activityLogRepository
                .countActivitiesPerDayByUserAndDateRange(callerId, fromDate, toDate);

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
