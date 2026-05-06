package com.infy.service;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.entity.Challenge;
import com.infy.entity.ChallengeParticipant;
import com.infy.enums.NotificationType;
import com.infy.repository.ChallengeParticipantRepository;
import com.infy.repository.ChallengeRepository;
import com.infy.repository.NotificationRepository;

@Service
public class ChallengeStatusSyncService {

    private static final Log LOGGER = LogFactory.getLog(ChallengeStatusSyncService.class);

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeParticipantRepository participantRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    // Syncs all stale challenge statuses in the DB in one round trip.
    @Transactional
    public void syncStatuses() {
        LocalDate today = LocalDate.now();
        challengeRepository.activateStartedChallenges(today);
        challengeRepository.completeExpiredChallenges(today);
    }

    // Syncs statuses AND notifies participants of challenges that became ACTIVE today.
    // Called only from getActiveChallenges() and getMyChallenges().
    // Notification block is fully silent — failure never breaks the sync or caller.
    @Transactional
    public void syncAndNotifyActivations() {
        LocalDate today = LocalDate.now();
        challengeRepository.activateStartedChallenges(today);
        challengeRepository.completeExpiredChallenges(today);

        try {
            List<Challenge> activatedToday = challengeRepository.findActivatedToday(today);

            for (Challenge challenge : activatedToday) {
                Integer challengeId = challenge.getChallengeId();
                String title   = "'" + challenge.getTitle() + "' Has Started!";
                String message = "The challenge '" + challenge.getTitle()
                        + "' is now active. Start logging to contribute!";

                List<ChallengeParticipant> participants = participantRepository
                        .findByChallenge_ChallengeIdOrderByJoinedAtAsc(challengeId);

                for (ChallengeParticipant participant : participants) {
                    Integer userId = participant.getUser().getUserId();

                    // Stable dedup — keyed on userId + CHALLENGE + challengeId (referenceId).
                    // Title-based check removed (P1 fix).
                    boolean alreadyNotified = notificationRepository
                            .existsByUser_UserIdAndNotificationTypeAndReferenceId(
                                    userId, NotificationType.CHALLENGE, challengeId);

                    if (!alreadyNotified) {
                        // Pass challengeId as referenceId so it is persisted
                        // and available for future dedup checks.
                        notificationService.createNotification(
                                userId, NotificationType.CHALLENGE,
                                title, message, challengeId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Activation notification failed during syncAndNotifyActivations", e);
        }
    }
}
