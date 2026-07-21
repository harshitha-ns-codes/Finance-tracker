package com.example.financetracker.health.narrative;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.FinancialAdvisoryContextFactory;
import com.example.financetracker.health.advisory.dto.BudgetPredictionDto;
import com.example.financetracker.health.advisory.dto.CategoryPredictionDto;
import com.example.financetracker.health.advisory.service.PredictionService;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.service.HealthScoreContextFactory;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NarrativeService {

    private static final BigDecimal LARGE_TX_MULTIPLIER = new BigDecimal("2.5");

    private final CurrentUserService currentUserService;
    private final HealthScoreContextFactory contextFactory;
    private final HealthScoreAggregator aggregator;
    private final FinancialAdvisoryContextFactory advisoryContextFactory;
    private final PredictionService predictionService;

    public NarrativeService(
            CurrentUserService currentUserService,
            HealthScoreContextFactory contextFactory,
            HealthScoreAggregator aggregator,
            FinancialAdvisoryContextFactory advisoryContextFactory,
            PredictionService predictionService) {
        this.currentUserService = currentUserService;
        this.contextFactory = contextFactory;
        this.aggregator = aggregator;
        this.advisoryContextFactory = advisoryContextFactory;
        this.predictionService = predictionService;
    }

    @Transactional(readOnly = true)
    public NarrativeResponse build(String monthStr) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(monthStr);
        HealthScoreContext ctx = contextFactory.build(user, month);
        List<CategoryScoreDto> breakdown = aggregator.calculateAll(ctx);
        int score = aggregator.totalScore(breakdown);

        BudgetPredictionDto prediction = null;
        try {
            FinancialAdvisoryContext advisoryCtx = advisoryContextFactory.build(user, month);
            prediction = predictionService.predict(advisoryCtx);
        } catch (Exception ignored) {
            // Prediction is optional for the narrative
        }

        List<String> sentences = new ArrayList<>();
        sentences.add(incomeExpenseSentence(ctx, month));
        String budgetSentence = budgetStatusSentence(ctx, month);
        if (budgetSentence != null) {
            sentences.add(budgetSentence);
        }
        sentences.add(savingsRateSentence(ctx));
        String anomaly = anomalySentence(ctx);
        if (anomaly != null) {
            sentences.add(anomaly);
        }
        sentences.add(scoreChangeSentence(score, ctx.getPreviousScore()));
        String predictionSentence = predictionSentence(prediction);
        if (predictionSentence != null) {
            sentences.add(predictionSentence);
        }

        String narrative = String.join(" ", sentences);
        String tone = resolveTone(score, prediction, anomaly != null, ctx);
        return new NarrativeResponse(narrative, tone);
    }

    private String incomeExpenseSentence(HealthScoreContext ctx, YearMonth month) {
        BigDecimal income = money(ctx.getMonthIncome());
        BigDecimal expenses = money(ctx.getMonthExpenses());
        boolean hasBudget = ctx.getBudgetItems() != null && !ctx.getBudgetItems().isEmpty();
        int remaining = remainingDays(month);
        if (!hasBudget && remaining > 0) {
            return "This month you've earned " + inr(income)
                    + " and spent " + inr(expenses)
                    + " with " + remaining + " day" + (remaining == 1 ? "" : "s") + " remaining.";
        }
        return "This month you've earned " + inr(income)
                + " and spent " + inr(expenses) + ".";
    }

    private String budgetStatusSentence(HealthScoreContext ctx, YearMonth month) {
        Map<String, BigDecimal> planned = new HashMap<>();
        for (CategoryBudgetItem item : ctx.getBudgetItems()) {
            planned.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }
        if (planned.isEmpty()) {
            return null;
        }

        String topCategory = null;
        double topPct = -1;
        BigDecimal topSpent = BigDecimal.ZERO;
        BigDecimal topBudget = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : planned.entrySet()) {
            BigDecimal budget = entry.getValue();
            if (budget.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal spent = ctx.getSpentByCategory().getOrDefault(entry.getKey(), BigDecimal.ZERO);
            double pct = spent
                    .multiply(new BigDecimal("100"))
                    .divide(budget, 1, RoundingMode.HALF_UP)
                    .doubleValue();
            if (pct > topPct) {
                topPct = pct;
                topCategory = entry.getKey();
                topSpent = spent;
                topBudget = budget;
            }
        }

        if (topCategory == null) {
            return null;
        }

        int remaining = remainingDays(month);
        String remainingPart = remaining > 0
                ? " with " + remaining + " day" + (remaining == 1 ? "" : "s") + " remaining"
                : "";
        return "You're at " + Math.round(topPct) + "% of your " + topCategory
                + " budget (" + inr(topSpent) + " of " + inr(topBudget) + ")"
                + remainingPart + ".";
    }

    private String savingsRateSentence(HealthScoreContext ctx) {
        BigDecimal income = ctx.getMonthIncome();
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return "Savings rate isn't available yet — add income transactions to unlock it.";
        }
        BigDecimal savings = ctx.monthSavings();
        double rate = savings
                .multiply(new BigDecimal("100"))
                .divide(income, 0, RoundingMode.HALF_UP)
                .doubleValue();
        String quality;
        if (rate >= 30) {
            quality = "excellent";
        } else if (rate >= 20) {
            quality = "strong";
        } else if (rate >= 10) {
            quality = "okay";
        } else if (rate >= 0) {
            quality = "low";
        } else {
            quality = "negative — you're spending more than you earn";
        }
        if (rate < 0) {
            return "Your savings rate is " + Math.round(rate) + "%, which is " + quality + ".";
        }
        return "Your savings rate of " + Math.round(rate) + "% is " + quality + ".";
    }

    private String anomalySentence(HealthScoreContext ctx) {
        List<Transaction> expenses = ctx.getMonthTransactions().stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();
        if (expenses.size() < 2) {
            return null;
        }
        BigDecimal avg = expenses.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(expenses.size()), 2, RoundingMode.HALF_UP);
        BigDecimal threshold = avg.multiply(LARGE_TX_MULTIPLIER);

        Transaction largest = expenses.stream()
                .max(Comparator.comparing(Transaction::getAmount))
                .orElse(null);
        if (largest == null || largest.getAmount().compareTo(threshold) <= 0) {
            return null;
        }
        String label = largest.getDescription() != null && !largest.getDescription().isBlank()
                ? largest.getDescription()
                : largest.getCategory();
        return "An unusual expense stood out: " + label + " at " + inr(largest.getAmount())
                + " (well above your average of " + inr(avg) + ").";
    }

    private String scoreChangeSentence(int score, Integer previous) {
        if (previous == null) {
            return "Your financial health score is " + score + "/100.";
        }
        int delta = score - previous;
        if (delta > 0) {
            return "Your financial health score is " + score + "/100, up " + delta
                    + " point" + (delta == 1 ? "" : "s") + " from last month.";
        }
        if (delta < 0) {
            int abs = Math.abs(delta);
            return "Your financial health score is " + score + "/100, down " + abs
                    + " point" + (abs == 1 ? "" : "s") + " from last month.";
        }
        return "Your financial health score is " + score + "/100, unchanged from last month.";
    }

    private String predictionSentence(BudgetPredictionDto prediction) {
        if (prediction == null || prediction.getCategories() == null || prediction.getCategories().isEmpty()) {
            return null;
        }
        CategoryPredictionDto worst = prediction.getCategories().stream()
                .filter(c -> c.getOverspendAmount() != null
                        && c.getOverspendAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(CategoryPredictionDto::getOverspendAmount))
                .orElse(null);

        if (worst != null) {
            return "Watch out for your " + worst.getCategory() + " category — at this rate you'll exceed budget by "
                    + inr(worst.getOverspendAmount()) + " by month end.";
        }

        if ("HIGH".equals(prediction.getRiskLevel()) || "CRITICAL".equals(prediction.getRiskLevel())) {
            return "Overall spending risk is " + prediction.getRiskLevel()
                    + " — tightening discretionary spend this week would help.";
        }

        if (prediction.getExpectedSavings() != null
                && prediction.getExpectedSavings().compareTo(BigDecimal.ZERO) > 0) {
            return "At the current pace you're on track to save about "
                    + inr(prediction.getExpectedSavings()) + " by month end.";
        }

        return "Keep the current pace and you should finish the month within budget.";
    }

    private String resolveTone(int score,
                               BudgetPredictionDto prediction,
                               boolean hasAnomaly,
                               HealthScoreContext ctx) {
        boolean criticalRisk = prediction != null
                && ("CRITICAL".equals(prediction.getRiskLevel()));
        boolean highOverspend = prediction != null && prediction.getCategories() != null
                && prediction.getCategories().stream()
                .anyMatch(c -> c.getOverspendAmount() != null
                        && c.getOverspendAmount().compareTo(new BigDecimal("500")) > 0
                        && ("HIGH".equals(c.getRiskLevel()) || "CRITICAL".equals(c.getRiskLevel())));
        boolean negativeSavings = ctx.getMonthIncome().compareTo(BigDecimal.ZERO) > 0
                && ctx.monthSavings().compareTo(BigDecimal.ZERO) < 0;

        if (score < 50 || criticalRisk || (negativeSavings && score < 65)) {
            return "critical";
        }
        if (score < 65 || hasAnomaly || highOverspend
                || (prediction != null && ("HIGH".equals(prediction.getRiskLevel())
                || "MEDIUM".equals(prediction.getRiskLevel())))) {
            return "warning";
        }
        return "positive";
    }

    private static int remainingDays(YearMonth month) {
        LocalDate today = LocalDate.now();
        if (!YearMonth.from(today).equals(month)) {
            if (month.isBefore(YearMonth.from(today))) {
                return 0;
            }
            return month.lengthOfMonth();
        }
        return Math.max(0, month.atEndOfMonth().getDayOfMonth() - today.getDayOfMonth());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String inr(BigDecimal value) {
        BigDecimal v = money(value).setScale(0, RoundingMode.HALF_UP);
        return "₹" + String.format("%,d", v.longValue());
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }
    }
}
