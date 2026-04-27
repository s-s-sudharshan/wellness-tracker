package com.infy.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infy.repository.ChallengeRepository;

@Service
public class ChallengeStatusSyncService {

    @Autowired
    private ChallengeRepository challengeRepository;

    // Syncs all stale challenge statuses in the DB in one round trip.
    // Called at the start of any service method that reads or lists challenges.
    // Two UPDATE queries: one for UPCOMING->ACTIVE, one for */->COMPLETED.
    @Transactional
    public void syncStatuses() {
        LocalDate today = LocalDate.now();
        challengeRepository.activateStartedChallenges(today);
        challengeRepository.completeExpiredChallenges(today);
    }
}
