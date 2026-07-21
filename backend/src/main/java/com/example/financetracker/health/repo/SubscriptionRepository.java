package com.example.financetracker.health.repo;

import com.example.financetracker.health.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserIdAndActiveTrue(Long userId);
}
