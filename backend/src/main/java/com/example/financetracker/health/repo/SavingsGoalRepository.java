package com.example.financetracker.health.repo;

import com.example.financetracker.health.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByUserIdAndActiveTrue(Long userId);

    List<SavingsGoal> findByUserIdAndLinkedCategoryAndActiveTrue(Long userId, String linkedCategory);

    List<SavingsGoal> findByUserIdAndLinkedCategoryIsNotNullAndActiveTrue(Long userId);
}
