package com.example.financetracker.health.repo;

import com.example.financetracker.health.model.HealthScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HealthScoreSnapshotRepository extends JpaRepository<HealthScoreSnapshot, Long> {
    Optional<HealthScoreSnapshot> findByUserIdAndScoreMonth(Long userId, String scoreMonth);
}
