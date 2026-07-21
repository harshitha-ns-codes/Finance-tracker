package com.example.financetracker.health.service;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.budget.CategoryBudgetItemRepository;
import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.HealthScoreSnapshot;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.model.Subscription;
import com.example.financetracker.health.repo.FinancialProfileRepository;
import com.example.financetracker.health.repo.HealthScoreSnapshotRepository;
import com.example.financetracker.health.repo.SavingsGoalRepository;
import com.example.financetracker.health.repo.SubscriptionRepository;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.transaction.TransactionRepository;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HealthScoreContextFactory {

    private final TransactionRepository transactionRepository;
    private final CategoryBudgetItemRepository budgetItemRepository;
    private final CategoryBudgetService categoryBudgetService;
    private final FinancialProfileRepository profileRepository;
    private final SavingsGoalRepository goalRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final HealthScoreSnapshotRepository snapshotRepository;

    public HealthScoreContextFactory(
            TransactionRepository transactionRepository,
            CategoryBudgetItemRepository budgetItemRepository,
            CategoryBudgetService categoryBudgetService,
            FinancialProfileRepository profileRepository,
            SavingsGoalRepository goalRepository,
            SubscriptionRepository subscriptionRepository,
            HealthScoreSnapshotRepository snapshotRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.categoryBudgetService = categoryBudgetService;
        this.profileRepository = profileRepository;
        this.goalRepository = goalRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public HealthScoreContext build(User user, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Transaction> monthTx = transactionRepository.findByUserAndDateBetween(user, from, to);

        YearMonth seriesStart = month.minusMonths(5);
        List<Transaction> recent = transactionRepository.findByUserAndDateBetween(
                user, seriesStart.atDay(1), to);

        BigDecimal income = sum(monthTx, TransactionType.INCOME);
        // Single source of truth with Budget / Dashboard: txs + spent overrides
        Map<String, BigDecimal> spentByCat = categoryBudgetService.effectiveSpentByCategory(user, month);
        BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, month);

        List<BigDecimal> monthlyExpenses = new ArrayList<>();
        for (YearMonth ym = seriesStart; !ym.isAfter(month); ym = ym.plusMonths(1)) {
            monthlyExpenses.add(categoryBudgetService.totalEffectiveSpent(user, ym));
        }

        List<CategoryBudgetItem> budgets =
                budgetItemRepository.findByUserAndMonthOrderByCategoryAscIdAsc(user, month);

        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        List<SavingsGoal> goals = goalRepository.findByUserIdAndActiveTrue(user.getId());
        List<Subscription> subs = subscriptionRepository.findByUserIdAndActiveTrue(user.getId());

        Integer previous = snapshotRepository
                .findByUserIdAndScoreMonth(user.getId(), month.minusMonths(1).toString())
                .map(HealthScoreSnapshot::getScore)
                .orElse(null);

        return new HealthScoreContext(
                user, month, monthTx, recent, budgets, spentByCat,
                income, expenses, monthlyExpenses, profile, goals, subs, previous
        );
    }

    private static BigDecimal sum(List<Transaction> txs, TransactionType type) {
        return txs.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
