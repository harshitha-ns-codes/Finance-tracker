package com.example.financetracker.health.repo;

import com.example.financetracker.health.model.PlannedPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlannedPurchaseRepository extends JpaRepository<PlannedPurchase, Long> {
    List<PlannedPurchase> findByUserIdAndActiveTrue(Long userId);
}
