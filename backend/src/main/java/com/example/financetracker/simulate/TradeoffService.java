package com.example.financetracker.simulate;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.FinancialAdvisoryContextFactory;
import com.example.financetracker.health.advisory.dto.ImpactAnalysisDto;
import com.example.financetracker.health.advisory.simulation.FinancialSimulator;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.service.HealthScoreContextFactory;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TradeoffService {

    private final CurrentUserService currentUserService;
    private final HealthScoreContextFactory healthContextFactory;
    private final FinancialAdvisoryContextFactory advisoryContextFactory;
    private final HealthScoreAggregator aggregator;
    private final FinancialSimulator financialSimulator;

    public TradeoffService(
            CurrentUserService currentUserService,
            HealthScoreContextFactory healthContextFactory,
            FinancialAdvisoryContextFactory advisoryContextFactory,
            HealthScoreAggregator aggregator,
            FinancialSimulator financialSimulator) {
        this.currentUserService = currentUserService;
        this.healthContextFactory = healthContextFactory;
        this.advisoryContextFactory = advisoryContextFactory;
        this.aggregator = aggregator;
        this.financialSimulator = financialSimulator;
    }

    @Transactional(readOnly = true)
    public TradeoffResponse compare(TradeoffRequest request) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = YearMonth.now();
        HealthScoreContext health = healthContextFactory.build(user, month);
        FinancialAdvisoryContext advisoryCtx = advisoryContextFactory.build(user, month);

        List<CategoryScoreDto> breakdown = aggregator.calculateAll(health);
        int currentScore = aggregator.totalScore(breakdown);

        TradeoffResponse.TradeoffOptionResult option1 =
                evaluate(request.getOption1(), health, advisoryCtx, currentScore);
        TradeoffResponse.TradeoffOptionResult option2 =
                evaluate(request.getOption2(), health, advisoryCtx, currentScore);

        String winner = pickWinner(option1, option2);
        String reason = buildRecommendationReason(option1, option2, winner);

        TradeoffResponse response = new TradeoffResponse();
        response.setOption1(option1);
        response.setOption2(option2);
        response.setRecommendation(winner);
        response.setRecommendationReason(reason);
        return response;
    }

    private TradeoffResponse.TradeoffOptionResult evaluate(
            TradeoffRequest.TradeoffOptionInput input,
            HealthScoreContext health,
            FinancialAdvisoryContext advisoryCtx,
            int currentScore) {

        String type = normalizeType(input.getType());
        String name = input.getName() != null && !input.getName().isBlank()
                ? input.getName().trim()
                : ("PURCHASE".equals(type) ? "Purchase" : "Saving");
        BigDecimal amount = input.getAmount() != null ? input.getAmount() : BigDecimal.ZERO;

        TradeoffResponse.TradeoffOptionResult result = new TradeoffResponse.TradeoffOptionResult();
        result.setName(name);
        result.setType(type);

        if ("SAVING".equals(type)) {
            return evaluateSaving(result, name, amount, health, currentScore);
        }
        return evaluatePurchase(result, name, amount, health, advisoryCtx, currentScore);
    }

    private TradeoffResponse.TradeoffOptionResult evaluatePurchase(
            TradeoffResponse.TradeoffOptionResult result,
            String name,
            BigDecimal amount,
            HealthScoreContext health,
            FinancialAdvisoryContext advisoryCtx,
            int currentScore) {

        String category = guessPurchaseCategory(name);
        ImpactAnalysisDto impact = financialSimulator.simulatePurchase(advisoryCtx, amount, category);

        BigDecimal categoryBudget = advisoryCtx.getCategoryBudgets()
                .getOrDefault(category, BigDecimal.ZERO);
        BigDecimal categorySpent = health.getSpentByCategory()
                .getOrDefault(category, BigDecimal.ZERO);
        BigDecimal after = categorySpent.add(amount);
        boolean exceeds = categoryBudget.signum() > 0 && after.compareTo(categoryBudget) > 0;
        BigDecimal exceedBy = exceeds ? after.subtract(categoryBudget) : BigDecimal.ZERO;

        int scoreImpact = -4;
        if (exceeds) scoreImpact -= 6;
        if (impact.getBalanceAfterPurchase() != null
                && impact.getBalanceAfterPurchase().compareTo(BigDecimal.ZERO) < 0) {
            scoreImpact -= 5;
        }
        if (impact.getSavingsGoalDelayDays() > 14) scoreImpact -= 3;
        scoreImpact = Math.max(-25, Math.min(5, scoreImpact));

        result.setImmediateBalanceImpact(money(amount.negate()));
        result.setMonthlyImpact(money(amount.negate()));
        result.setHealthScoreImpact(scoreImpact);
        result.setTimeToRecover(purchaseRecoverText(amount, health, impact));

        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        pros.add("Immediate satisfaction");
        if (looksLikeWorkNeed(name)) {
            pros.add("Useful for work or daily needs");
        } else {
            pros.add("You get the item you want now");
        }

        if (exceeds) {
            cons.add("Exceeds " + category + " budget by ₹"
                    + exceedBy.setScale(0, RoundingMode.HALF_UP).toPlainString());
        } else if (categoryBudget.signum() > 0) {
            cons.add("Uses " + category + " budget capacity this month");
        }
        int delayDays = impact.getSavingsGoalDelayDays();
        if (delayDays > 0 && !health.getGoals().isEmpty()) {
            String goalName = health.getGoals().get(0).getName();
            cons.add("Delays " + goalName + " by about " + formatDuration(delayDays));
        } else {
            cons.add("Reduces this month's savings cushion");
        }
        if (impact.getBalanceAfterPurchase() != null
                && impact.getBalanceAfterPurchase().compareTo(BigDecimal.ZERO) < 0) {
            cons.add("Spendable balance would go negative");
        }

        result.setPros(pros);
        result.setCons(cons);
        return result;
    }

    private TradeoffResponse.TradeoffOptionResult evaluateSaving(
            TradeoffResponse.TradeoffOptionResult result,
            String name,
            BigDecimal amount,
            HealthScoreContext health,
            int currentScore) {

        result.setImmediateBalanceImpact(money(amount.negate()));
        result.setMonthlyImpact(money(amount.negate()));

        int scoreImpact = 8;
        SavingsGoal targetGoal = pickGoal(health, name);
        boolean emergencyRelated = isEmergencyRelated(name, targetGoal);

        if (emergencyRelated) scoreImpact += 4;
        if (health.getMonthIncome().signum() > 0) {
            double pct = amount.multiply(BigDecimal.valueOf(100))
                    .divide(health.getMonthIncome(), 1, RoundingMode.HALF_UP)
                    .doubleValue();
            if (pct >= 20) scoreImpact += 2;
        }
        scoreImpact = Math.max(4, Math.min(20, scoreImpact));
        result.setHealthScoreImpact(scoreImpact);
        result.setTimeToRecover(savingProgressText(amount, health, targetGoal));

        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();
        pros.add("Improves health score by about " + scoreImpact + " points");
        if (targetGoal != null) {
            pros.add("Moves you closer to " + targetGoal.getName());
        }
        if (emergencyRelated) {
            pros.add("Builds your financial safety net");
        } else {
            pros.add("Strengthens long-term financial resilience");
        }

        cons.add("Delayed gratification");
        cons.add("Cash is tied up and not available for spending now");

        result.setPros(pros);
        result.setCons(cons);
        return result;
    }

    private static String pickWinner(
            TradeoffResponse.TradeoffOptionResult a,
            TradeoffResponse.TradeoffOptionResult b) {
        int scoreA = a.getHealthScoreImpact();
        int scoreB = b.getHealthScoreImpact();
        if (scoreB != scoreA) {
            return scoreB > scoreA ? "option2" : "option1";
        }
        // Prefer saving when scores tie
        if ("SAVING".equals(b.getType()) && !"SAVING".equals(a.getType())) {
            return "option2";
        }
        if ("SAVING".equals(a.getType()) && !"SAVING".equals(b.getType())) {
            return "option1";
        }
        return "option1";
    }

    private static String buildRecommendationReason(
            TradeoffResponse.TradeoffOptionResult a,
            TradeoffResponse.TradeoffOptionResult b,
            String winner) {
        TradeoffResponse.TradeoffOptionResult win = "option2".equals(winner) ? b : a;
        TradeoffResponse.TradeoffOptionResult lose = "option2".equals(winner) ? a : b;
        String winSign = win.getHealthScoreImpact() >= 0 ? "+" : "";
        String loseSign = lose.getHealthScoreImpact() >= 0 ? "+" : "";

        if ("SAVING".equals(win.getType())) {
            return "Choosing \"" + win.getName() + "\" has a net health score impact of "
                    + winSign + win.getHealthScoreImpact()
                    + " vs " + loseSign + lose.getHealthScoreImpact()
                    + " for \"" + lose.getName()
                    + "\", and puts you closer to your financial safety net.";
        }
        return "\"" + win.getName() + "\" looks like the better trade-off right now "
                + "(health impact " + winSign + win.getHealthScoreImpact()
                + " vs " + loseSign + lose.getHealthScoreImpact()
                + " for \"" + lose.getName() + "\").";
    }

    private static String purchaseRecoverText(
            BigDecimal amount, HealthScoreContext health, ImpactAnalysisDto impact) {
        BigDecimal monthlySavings = health.monthSavings().max(BigDecimal.ZERO);
        if (monthlySavings.signum() <= 0) {
            int delay = impact.getSavingsGoalDelayDays();
            if (delay > 0) {
                return "About " + formatDuration(delay) + " of goal delay to absorb this purchase";
            }
            return "Harder to rebuild savings with current monthly surplus";
        }
        int months = amount.divide(monthlySavings, 0, RoundingMode.CEILING).intValue();
        months = Math.max(1, Math.min(months, 24));
        return months + " month" + (months == 1 ? "" : "s") + " to rebuild savings";
    }

    private static String savingProgressText(
            BigDecimal amount, HealthScoreContext health, SavingsGoal goal) {
        if (goal == null) {
            return "Builds reserves immediately";
        }
        BigDecimal remaining = goal.getTargetAmount()
                .subtract(goal.getCurrentAmount())
                .max(BigDecimal.ZERO);
        if (remaining.signum() <= 0) {
            return "Goal already reached — boosts your cushion further";
        }
        BigDecimal after = remaining.subtract(amount).max(BigDecimal.ZERO);
        if (after.signum() <= 0) {
            return "Could complete \"" + goal.getName() + "\" with this contribution";
        }
        BigDecimal monthly = health.monthSavings().max(amount).max(BigDecimal.ONE);
        int months = after.divide(monthly, 0, RoundingMode.CEILING).intValue();
        months = Math.max(1, Math.min(months, 36));
        return "Reaches \"" + goal.getName() + "\" in about " + months
                + " month" + (months == 1 ? "" : "s") + " at this pace";
    }

    private static SavingsGoal pickGoal(HealthScoreContext health, String name) {
        List<SavingsGoal> goals = health.getGoals();
        if (goals == null || goals.isEmpty()) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        for (SavingsGoal g : goals) {
            if (g.getName() != null && lower.contains(g.getName().toLowerCase(Locale.ROOT))) {
                return g;
            }
        }
        if (lower.contains("emergency")) {
            for (SavingsGoal g : goals) {
                if (g.getName() != null
                        && g.getName().toLowerCase(Locale.ROOT).contains("emergency")) {
                    return g;
                }
            }
        }
        return goals.get(0);
    }

    private static boolean isEmergencyRelated(String name, SavingsGoal goal) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("emergency") || lower.contains("safety")) return true;
        return goal != null && goal.getName() != null
                && goal.getName().toLowerCase(Locale.ROOT).contains("emergency");
    }

    private static String guessPurchaseCategory(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("food") || lower.contains("dinner") || lower.contains("lunch")) {
            return "Food";
        }
        if (lower.contains("uber") || lower.contains("fuel") || lower.contains("metro")) {
            return "Transport";
        }
        if (lower.contains("movie") || lower.contains("game") || lower.contains("netflix")) {
            return "Entertainment";
        }
        if (lower.contains("course") || lower.contains("book") || lower.contains("class")) {
            return "Education";
        }
        if (lower.contains("sip") || lower.contains("invest")) {
            return "Investments/Savings";
        }
        return "Shopping";
    }

    private static boolean looksLikeWorkNeed(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("work") || lower.contains("office") || lower.contains("laptop")
                || lower.contains("shoes") || lower.contains("commute");
    }

    private static String normalizeType(String raw) {
        if (raw == null) return "PURCHASE";
        String t = raw.trim().toUpperCase(Locale.ROOT);
        return "SAVING".equals(t) || "SAVE".equals(t) || "SAVINGS".equals(t)
                ? "SAVING"
                : "PURCHASE";
    }

    private static String formatDuration(int days) {
        if (days >= 7) {
            int weeks = Math.max(1, days / 7);
            return weeks + " week" + (weeks == 1 ? "" : "s");
        }
        return days + " day" + (days == 1 ? "" : "s");
    }

    private static BigDecimal money(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
