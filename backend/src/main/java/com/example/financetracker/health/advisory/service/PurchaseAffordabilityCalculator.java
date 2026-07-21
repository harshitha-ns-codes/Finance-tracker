package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.advisory.AdvisoryMath;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.ImpactAnalysisDto;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.model.FinancialProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PurchaseAffordabilityCalculator {

    private final HealthScoreProperties props;

    public PurchaseAffordabilityCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    public record AffordabilityResult(int totalScore, Map<String, Integer> componentScores) {}

    public AffordabilityResult calculate(
            FinancialAdvisoryContext ctx,
            ImpactAnalysisDto impact,
            BigDecimal price,
            String priority) {

        var aff = props.getAffordability();
        Map<String, Integer> components = new LinkedHashMap<>();
        var health = ctx.getHealthContext();

        BigDecimal rawBudget = ctx.getCategoryBudgets().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pool = AdvisoryMath.discretionaryPool(rawBudget, health, ctx.getCurrentBalance());

        double remainingPct = pool.signum() > 0
                ? impact.getRemainingBudgetAfter().max(BigDecimal.ZERO)
                .divide(pool, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : (impact.getRemainingBudgetAfter().signum() >= 0 ? 50 : 0);
        components.put("Remaining Budget", ScoreMath.scalePoints(remainingPct, 0, 100, aff.getRemainingBudgetWeight()));

        components.put("Emergency Fund", ScoreMath.scalePoints(
                impact.getEmergencyFundMonthsAfter(),
                aff.getMinEmergencyMonths(),
                aff.getTargetEmergencyMonths(),
                aff.getEmergencyFundWeight()));

        BigDecimal unpaidBills = unpaidBillsTotal(ctx);
        int billsScore;
        if (unpaidBills.signum() <= 0) {
            billsScore = aff.getUpcomingBillsWeight();
        } else {
            double coveredRatio = impact.getBalanceAfterPurchase()
                    .divide(unpaidBills, 4, RoundingMode.HALF_UP).doubleValue();
            billsScore = ScoreMath.scalePoints(Math.min(1, coveredRatio), 0, 1, aff.getUpcomingBillsWeight());
        }
        components.put("Upcoming Bills", billsScore);

        int delayDays = impact.getSavingsGoalDelayDays();
        int goalScore = delayDays <= 0 ? aff.getSavingsGoalWeight()
                : ScoreMath.scalePoints(Math.max(0, 30 - delayDays), 0, 30, aff.getSavingsGoalWeight());
        components.put("Savings Goal", goalScore);

        double overspendRatio = historicalOverspendRatio(ctx);
        components.put("Overspending History", ScoreMath.scalePoints(
                1 - overspendRatio, 0, 1, aff.getOverspendingHistoryWeight()));

        double cv = spendingCoefficientOfVariation(ctx.getHealthContext().getMonthlyExpenseSeries());
        components.put("Cash Flow Stability", ScoreMath.scalePoints(
                cv, props.getSpendingConsistency().getMaxCvPercent(),
                props.getSpendingConsistency().getIdealCvPercent(),
                aff.getCashFlowStabilityWeight()));

        int total = components.values().stream().mapToInt(Integer::intValue).sum();
        total += priorityAdjustment(priority);

        return new AffordabilityResult(Math.max(0, Math.min(100, total)), components);
    }

    private int priorityAdjustment(String priority) {
        if (priority == null) return 0;
        return switch (priority.toUpperCase()) {
            case "NECESSITY" -> props.getAffordability().getNecessityBonus();
            case "LUXURY" -> -props.getAffordability().getLuxuryPenalty();
            default -> 0;
        };
    }

    private static BigDecimal unpaidBillsTotal(FinancialAdvisoryContext ctx) {
        return ctx.getBudgetItems().stream()
                .filter(i -> (i.isFixed() || i.getDueDate() != null) && !i.isPaid())
                .map(i -> i.getPlannedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double historicalOverspendRatio(FinancialAdvisoryContext ctx) {
        BigDecimal totalBudget = ctx.getCategoryBudgets().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalBudget.signum() <= 0) return 0;

        HealthScoreContext health = ctx.getHealthContext();
        List<BigDecimal> series = health.getMonthlyExpenseSeries();
        if (series.size() < 2) return 0;

        long overCount = series.subList(0, series.size() - 1).stream()
                .filter(exp -> exp.compareTo(totalBudget) > 0)
                .count();
        return (double) overCount / Math.max(1, series.size() - 1);
    }

    private double spendingCoefficientOfVariation(List<BigDecimal> series) {
        if (series == null || series.size() < 2) {
            return props.getSpendingConsistency().getIdealCvPercent();
        }
        double[] vals = series.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double mean = 0;
        for (double v : vals) mean += v;
        mean /= vals.length;
        if (mean <= 0) return 0;
        double variance = 0;
        for (double v : vals) variance += (v - mean) * (v - mean);
        variance /= vals.length;
        return (Math.sqrt(variance) / mean) * 100;
    }
}
