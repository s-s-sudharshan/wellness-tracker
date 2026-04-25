package com.infy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.infy.entity.ChallengeParticipant;

public interface ChallengeParticipantRepository extends CrudRepository<ChallengeParticipant, Integer> {

    // US 03 - Check if a user has already joined a specific challenge
    Optional<ChallengeParticipant> findByChallenge_ChallengeIdAndUser_UserId(
            Integer challengeId, Integer userId);

    // US 03 - Get all challenges a user has joined (My Challenges section)
    List<ChallengeParticipant> findByUser_UserIdOrderByJoinedAtDesc(Integer userId);

    // US 03 - Check if a user joined any of a list of challenges (for alreadyJoined flag)
    @Query("SELECT cp.challenge.challengeId FROM ChallengeParticipant cp " +
           "WHERE cp.user.userId = :userId " +
           "AND cp.challenge.challengeId IN :challengeIds")
    List<Integer> findJoinedChallengeIdsByUser(
            @Param("userId") Integer userId,
            @Param("challengeIds") List<Integer> challengeIds);

    // US 04 (prep) - Get all participants of a challenge for leaderboard
    List<ChallengeParticipant> findByChallenge_ChallengeIdOrderByJoinedAtAsc(Integer challengeId);
}
