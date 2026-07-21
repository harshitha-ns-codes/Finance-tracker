package com.example.financetracker.advisor;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.budget.CategoryBudgetItemRepository;
import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.service.HealthScoreContextFactory;
import com.example.financetracker.recurring.RecurringTransaction;
import com.example.financetracker.recurring.RecurringTransactionRepository;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

@Service
public class DailyAdvisorService {

    private final CurrentUserService currentUserService;
    private final CategoryBudgetService categoryBudgetService;
    private final CategoryBudgetItemRepository budgetItemRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final HealthScoreContextFactory healthContextFactory;
    private final HealthScoreAggregator aggregator;

    public DailyAdvisorService(
            CurrentUserService currentUserService,
            CategoryBudgetService categoryBudgetService,
            CategoryBudgetItemRepository budgetItemRepository,
            RecurringTransactionRepository recurringRepository,
            HealthScoreContextFactory healthContextFactory,
            HealthScoreAggregator aggregator) {
        this.currentUserService = currentUserService;
        this.categoryBudgetService = categoryBudgetService;
        this.budgetItemRepository = budgetItemRepository;
        this.recurringRepository = recurringRepository;
        this.healthContextFactory = healthContextFactory;
        this.aggregator = aggregator;
    }

    @Transactional(readOnly = true)
    public DailyInsightResponse todaysInsight() {
        User user = currentUserService.getCurrentUser();
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();

        HealthScoreContext health = healthContextFactory.build(user, month);
        int score = aggregator.totalScore(aggregator.calculateAll(health));

        DailyInsightResponse budget = budgetPaceWarning(user, month, today);
        if (budget != null) {
            return budget;
        }

        DailyInsightResponse recurring = upcomingRecurringReminder(user, month, today);
        if (recurring != null) {
            return recurring;
        }

        DailyInsightResponse savings = highSavingsAchievement(health);
        if (savings != null) {
            return savings;
        }

        DailyInsightResponse scoreUp = scoreImprovementAchievement(health, score);
        if (scoreUp != null) {
            return scoreUp;
        }

        return defaultTip();
    }

    /** 1. Category > 90% of budget with more than 5 days left in month. */
    private DailyInsightResponse budgetPaceWarning(User user, YearMonth month, LocalDate today) {
        if (!YearMonth.from(today).equals(month)) {
            return null;
        }

        int daysLeft = month.atEndOfMonth().getDayOfMonth() - today.getDayOfMonth();
        if (daysLeft <= 5) {
            return null;
        }

        Map<String, BigDecimal> spent = categoryBudgetService.effectiveSpentByCategory(user, month);
        Map<String, BigDecimal> planned = new java.util.HashMap<>();
        for (CategoryBudgetItem item :
                budgetItemRepository.findByUserAndMonthOrderByCategoryAscIdAsc(user, month)) {
            planned.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }

        String worstCat = null;
        double worstPct = 0;
        BigDecimal worstBudget = BigDecimal.ZERO;
        BigDecimal worstSpent = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : planned.entrySet()) {
            BigDecimal budget = entry.getValue();
            if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal used = spent.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            double pct = used.multiply(new BigDecimal("100"))
                    .divide(budget, 1, RoundingMode.HALF_UP)
                    .doubleValue();
            if (pct > worstPct) {
                worstPct = pct;
                worstCat = entry.getKey();
                worstBudget = budget;
                worstSpent = used;
            }
        }

        if (worstCat == null || worstPct <= 90) {
            return null;
        }

        BigDecimal remaining = worstBudget.subtract(worstSpent).max(BigDecimal.ZERO);
        BigDecimal dailyLimit = remaining.divide(BigDecimal.valueOf(daysLeft), 0, RoundingMode.DOWN);
        String categoryLower = worstCat.toLowerCase(Locale.ENGLISH);
        String insight = worstCat + " is at " + Math.round(worstPct)
                + "% of budget with " + daysLeft + " days left. Limit "
                + categoryLower + " spending to ₹"
                + dailyLimit.toPlainString() + "/day to stay safe.";

        return new DailyInsightResponse(
                "WARNING",
                insight,
                "Review Budget sheet",
                "/plan");
    }

    /** 2. Recurring transaction due in the next 3 days. */
    private DailyInsightResponse upcomingRecurringReminder(User user, YearMonth month, LocalDate today) {
        LocalDate windowEnd = today.plusDays(3);
        RecurringTransaction soonest = null;
        LocalDate soonestDate = null;

        for (RecurringTransaction item :
                recurringRepository.findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(user)) {
            int day = Math.min(Math.max(item.getDayOfMonth(), 1), month.lengthOfMonth());
            LocalDate due = month.atDay(day);
            if (due.isBefore(today) || due.isAfter(windowEnd)) {
                continue;
            }
            if (soonestDate == null || due.isBefore(soonestDate)) {
                soonestDate = due;
                soonest = item;
            }
        }

        if (soonest == null || soonestDate == null) {
            return null;
        }

        long daysUntil = ChronoUnit.DAYS.between(today, soonestDate);
        String when = daysUntil == 0
                ? "today"
                : daysUntil == 1
                ? "in 1 day"
                : "in " + daysUntil + " days";
        String amount = "₹" + soonest.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString();
        String insight = soonest.getName() + " of " + amount + " is due "
                + when + ". Make sure funds are ready.";

        return new DailyInsightResponse(
                "REMINDER",
                insight,
                "View recurring",
                "/plan");
    }

    /** 3. Savings rate > 40%. */
    private DailyInsightResponse highSavingsAchievement(HealthScoreContext health) {
        BigDecimal income = health.getMonthIncome();
        if (income == null || income.signum() <= 0) {
            return null;
        }

        double rate = health.monthSavings()
                .multiply(new BigDecimal("100"))
                .divide(income, 0, RoundingMode.HALF_UP)
                .doubleValue();
        if (rate <= 40) {
            return null;
        }

        String insight = "You're saving " + Math.round(rate)
                + "% of your income this month. Excellent discipline.";

        return new DailyInsightResponse("ACHIEVEMENT", insight, null, null);
    }

    /** 4. Health score improved vs last month. */
    private DailyInsightResponse scoreImprovementAchievement(HealthScoreContext health, int score) {
        Integer previous = health.getPreviousScore();
        if (previous == null) {
            return null;
        }

        int delta = score - previous;
        if (delta <= 0) {
            return null;
        }

        String insight = "Your financial health score is up " + delta
                + " point" + (delta == 1 ? "" : "s")
                + " from last month. Keep it up.";

        return new DailyInsightResponse("ACHIEVEMENT", insight, null, null);
    }

    /** 5. Default tip. */
    private DailyInsightResponse defaultTip() {
        return new DailyInsightResponse(
                "TIP",
                "Log your transactions daily for more accurate predictions and advisor insights.",
                "Add transaction",
                "/transactions");
    }
}
