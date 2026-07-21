package com.example.financetracker.health.web;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.model.Subscription;
import com.example.financetracker.health.repo.FinancialProfileRepository;
import com.example.financetracker.health.repo.SavingsGoalRepository;
import com.example.financetracker.health.repo.SubscriptionRepository;
import com.example.financetracker.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health-inputs")
public class HealthInputsController {

    private final CurrentUserService currentUserService;
    private final FinancialProfileRepository profileRepository;
    private final SavingsGoalRepository goalRepository;
    private final SubscriptionRepository subscriptionRepository;

    public HealthInputsController(
            CurrentUserService currentUserService,
            FinancialProfileRepository profileRepository,
            SavingsGoalRepository goalRepository,
            SubscriptionRepository subscriptionRepository) {
        this.currentUserService = currentUserService;
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<FinancialProfile> getProfile() {
        User user = currentUserService.getCurrentUser();
        FinancialProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    FinancialProfile p = new FinancialProfile();
                    p.setUserId(user.getId());
                    p.setEmergencyFundBalance(BigDecimal.ZERO);
                    p.setMonthlyDebtPayments(BigDecimal.ZERO);
                    p.setTotalDebtOutstanding(BigDecimal.ZERO);
                    p.setCurrentBalance(BigDecimal.ZERO);
                    p.setSalaryDayOfMonth(1);
                    return p;
                });
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<FinancialProfile> upsertProfile(@RequestBody Map<String, Object> body) {
        User user = currentUserService.getCurrentUser();
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElseGet(FinancialProfile::new);
        profile.setUserId(user.getId());
        if (body.get("emergencyFundBalance") != null) {
            profile.setEmergencyFundBalance(new BigDecimal(String.valueOf(body.get("emergencyFundBalance"))));
        }
        if (body.get("monthlyDebtPayments") != null) {
            profile.setMonthlyDebtPayments(new BigDecimal(String.valueOf(body.get("monthlyDebtPayments"))));
        }
        if (body.get("totalDebtOutstanding") != null) {
            profile.setTotalDebtOutstanding(new BigDecimal(String.valueOf(body.get("totalDebtOutstanding"))));
        }
        if (body.get("currentBalance") != null) {
            profile.setCurrentBalance(new BigDecimal(String.valueOf(body.get("currentBalance"))));
        }
        if (body.get("salaryDayOfMonth") != null) {
            profile.setSalaryDayOfMonth(Integer.parseInt(String.valueOf(body.get("salaryDayOfMonth"))));
        }
        return ResponseEntity.ok(profileRepository.save(profile));
    }

    @GetMapping("/goals")
    public ResponseEntity<List<SavingsGoal>> goals() {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(goalRepository.findByUserIdAndActiveTrue(user.getId()));
    }

    @PostMapping("/goals")
    @Transactional
    public ResponseEntity<SavingsGoal> createGoal(@RequestBody Map<String, Object> body) {
        User user = currentUserService.getCurrentUser();
        SavingsGoal goal = new SavingsGoal();
        goal.setUserId(user.getId());
        goal.setName(String.valueOf(body.getOrDefault("name", "Goal")));
        goal.setTargetAmount(new BigDecimal(String.valueOf(body.get("targetAmount"))));
        goal.setCurrentAmount(new BigDecimal(String.valueOf(body.getOrDefault("currentAmount", "0"))));
        goal.setDeadline(body.get("deadline") != null
                ? LocalDate.parse(String.valueOf(body.get("deadline")))
                : null);
        goal.setActive(true);
        return ResponseEntity.ok(goalRepository.save(goal));
    }

    @DeleteMapping("/goals/{id}")
    @Transactional
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        User user = currentUserService.getCurrentUser();
        SavingsGoal goal = goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        goal.setActive(false);
        goalRepository.save(goal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<Subscription>> subscriptions() {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.ok(subscriptionRepository.findByUserIdAndActiveTrue(user.getId()));
    }

    @PostMapping("/subscriptions")
    @Transactional
    public ResponseEntity<Subscription> createSub(@RequestBody Map<String, Object> body) {
        User user = currentUserService.getCurrentUser();
        Subscription s = new Subscription();
        s.setUserId(user.getId());
        s.setName(String.valueOf(body.getOrDefault("name", "Subscription")));
        s.setMonthlyAmount(new BigDecimal(String.valueOf(body.get("monthlyAmount"))));
        s.setUnused(Boolean.TRUE.equals(body.get("unused")));
        s.setDuplicate(Boolean.TRUE.equals(body.get("duplicate")));
        s.setActive(true);
        return ResponseEntity.ok(subscriptionRepository.save(s));
    }

    @DeleteMapping("/subscriptions/{id}")
    @Transactional
    public ResponseEntity<Void> deleteSub(@PathVariable Long id) {
        User user = currentUserService.getCurrentUser();
        Subscription s = subscriptionRepository.findById(id)
                .filter(x -> x.getUserId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
        s.setActive(false);
        subscriptionRepository.save(s);
        return ResponseEntity.noContent().build();
    }
}
