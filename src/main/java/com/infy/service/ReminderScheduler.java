package com.infy.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.infy.entity.ChallengeParticipant;
import com.infy.entity.User;
import com.infy.enums.ChallengeStatus;
import com.infy.enums.NotificationType;
import com.infy.enums.UserStatus;
import com.infy.repository.ActivityLogRepository;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.NotificationRepository;
import com.infy.repository.UserRepository;

@Component
public class ReminderScheduler {

    private static final Log LOGGER = LogFactory.getLog(ReminderScheduler.class);

    // Explicit timezone used in both scheduler trigger and date calculations.
    // Ensures LocalDate.now() always returns the correct IST date regardless
    // of server JVM default timezone.
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    // Namespaced referenceId keys — prevents dedup collisions between reminder types.
    // No-activity: negative date int  (e.g. -20260506)
    // Challenge-ending: positive challengeId (e.g. 3)
    private static final DateTimeFormatter DATE_KEY_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Reminder 1 — No Activity Logged Today
    // Runs at 7:00 PM IST daily.
    // Sends a REMINDER notification to every ACTIVE user who has logged zero
    // activities today. Dedup key: negative of today's date as yyyyMMdd integer.
    // -------------------------------------------------------------------------
    @Scheduled(cron = "0 55 22 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void remindInactiveUsers() {
        LOGGER.info("ReminderScheduler: starting no-activity reminder sweep.");

        // Fix 1: explicit IST timezone — consistent with scheduler trigger zone
        LocalDate today = LocalDate.now(IST);

        // Negative date int — namespace for no-activity reminders.
        // e.g. 2026-05-06 → key = -20260506
        int referenceId = -Integer.parseInt(today.format(DATE_KEY_FORMAT));

        String title   = "Don't forget to log your activity today!";
        String message = "You haven't logged any wellness activity today. "
                + "Even a short walk counts — keep your streak going!";

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);

        int sent    = 0;
        int skipped = 0;

        for (User user : activeUsers) {
            try {
                Integer userId = user.getUserId();

                // Re-check condition at send time — skip if user already logged today
                Integer count = activityLogRepository
                        .countByUser_UserIdAndActivityDateBetween(userId, today, today);
                if (count != null && count > 0) {
                    skipped++;
                    continue;
                }

                // Dedup guard — skip if this reminder was already sent today
                boolean alreadySent = notificationRepository
                        .existsByUser_UserIdAndNotificationTypeAndReferenceId(
                                userId, NotificationType.REMINDER, referenceId);
                if (alreadySent) {
                    skipped++;
                    continue;
                }

                notificationService.createNotification(
                        userId, NotificationType.REMINDER, title, message, referenceId);
                sent++;

            } catch (Exception e) {
                // One user failure must never stop the sweep for others
                LOGGER.error("No-activity reminder failed for userId="
                        + user.getUserId(), e);
            }
        }

        LOGGER.info("ReminderScheduler: no-activity reminder complete. "
                + "sent=" + sent + ", skipped=" + skipped);
    }

    // -------------------------------------------------------------------------
    // Reminder 2 — Challenge Ending Soon (Joined + Incomplete)
    // Runs at 9:00 AM IST daily.
    // Sends a REMINDER notification to every ACTIVE user who has joined a
    // challenge ending today or tomorrow AND whose progress is below 100%.
    // Dedup key: positive challengeId — fires once per challenge per user
    // across the 2-day window, which is intentional (less noise).
    // -------------------------------------------------------------------------
    @Scheduled(cron = "0 30 19 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void remindChallengeSoon() {
        LOGGER.info("ReminderScheduler: starting challenge-ending-soon reminder sweep.");

        // Fix 1: explicit IST timezone — consistent with scheduler trigger zone
        LocalDate today    = LocalDate.now(IST);
        LocalDate tomorrow = today.plusDays(1);

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);

        int sent    = 0;
        int skipped = 0;

        for (User user : activeUsers) {
            try {
                Integer userId = user.getUserId();

                List<ChallengeParticipant> participations =
                        participantRepository.findByUser_UserIdOrderByJoinedAtDesc(userId);

                for (ChallengeParticipant participation : participations) {
                    try {
                        LocalDate endDate = participation.getChallenge().getEndDate();

                        // Only challenges ending today or tomorrow qualify
                        if (!endDate.equals(today) && !endDate.equals(tomorrow)) {
                            continue;
                        }

                        // Fix 2: date-based guard against stale DB status column.
                        // syncStatuses() is not called here — a challenge whose endDate
                        // has passed but whose status is still ACTIVE in the DB would
                        // slip through the COMPLETED check below without this guard.
                        if (today.isAfter(endDate)) {
                            continue;
                        }

                        // Skip challenges already marked COMPLETED in DB
                        if (ChallengeStatus.COMPLETED.equals(
                                participation.getChallenge().getStatus())) {
                            continue;
                        }

                        Integer challengeId = participation.getChallenge().getChallengeId();

                        // Dedup guard — positive challengeId namespace.
                        // Fires once per challenge per user across the 2-day window.
                        boolean alreadySent = notificationRepository
                                .existsByUser_UserIdAndNotificationTypeAndReferenceId(
                                        userId, NotificationType.REMINDER, challengeId);
                        if (alreadySent) {
                            skipped++;
                            continue;
                        }

                        // Re-check progress at send time
                        LocalDate startDate  = participation.getChallenge().getStartDate();
                        LocalDate boundedEnd = endDate.isAfter(today) ? today : endDate;
                        Double goalValue     = participation.getChallenge().getGoalValue();

                        double actualValue = 0.0;
                        List<Object[]> actuals = activityLogRepository
                                .findActualsByUserAndDateRange(userId, startDate, boundedEnd);

                        for (Object[] row : actuals) {
                            // Known non-null side calls .equals() — row[0] may be null
                            if (participation.getChallenge().getMetricType().equals(row[0])) {
                                actualValue = ((Number) row[1]).doubleValue();
                                break;
                            }
                        }

                        // Skip if user already completed the challenge
                        int progressPct = calcPct(actualValue, goalValue);
                        if (progressPct >= 100) {
                            skipped++;
                            continue;
                        }

                        String challengeTitle = participation.getChallenge().getTitle();
                        String title;
                        if (endDate.equals(today)) {
                            title = "Last chance: '" + challengeTitle + "' ends today!";
                        } else {
                            title = "Final day tomorrow: '" + challengeTitle
                                    + "' ends tomorrow!";
                        }
                        String message = "You're at " + progressPct + "% of your goal. "
                                + "Log your activities now to finish strong!";

                        notificationService.createNotification(
                                userId, NotificationType.REMINDER,
                                title, message, challengeId);
                        sent++;

                    } catch (Exception e) {
                        // One challenge failure must never stop the rest
                        LOGGER.error("Challenge-ending reminder failed for userId="
                                + user.getUserId() + ", challengeId="
                                + participation.getChallenge().getChallengeId(), e);
                    }
                }

            } catch (Exception e) {
                // One user failure must never stop the sweep for others
                LOGGER.error("Challenge-ending reminder sweep failed for userId="
                        + user.getUserId(), e);
            }
        }

        LOGGER.info("ReminderScheduler: challenge-ending reminder complete. "
                + "sent=" + sent + ", skipped=" + skipped);
    }

    private int calcPct(double actual, Double goal) {
        if (goal == null || goal == 0) return 0;
        int pct = (int) ((actual / goal) * 100);
        return Math.min(pct, 100);
    }
}
