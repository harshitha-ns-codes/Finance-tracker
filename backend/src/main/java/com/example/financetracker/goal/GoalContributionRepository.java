package com.example.financetracker.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByDateDescIdDesc(Long goalId);

    boolean existsByGoalIdAndTransactionId(Long goalId, Long transactionId);
}
