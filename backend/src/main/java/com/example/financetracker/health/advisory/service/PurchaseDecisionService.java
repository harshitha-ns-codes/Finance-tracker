package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.advisory.AdvisoryMath;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.ImpactAnalysisDto;
import com.example.financetracker.health.advisory.dto.PurchaseDecisionDetailDto;
import com.example.financetracker.health.advisory.dto.PurchaseEvaluateDetailRequest;
import com.example.financetracker.health.advisory.simulation.FinancialSimulator;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.config.HealthScoreProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseDecisionService {

    private final FinancialSimulator simulator;
    private final PurchaseAffordabilityCalculator affordabilityCalculator;
    private final HealthScoreProperties props;

    public PurchaseDecisionService(
            FinancialSimulator simulator,
            PurchaseAffordabilityCalculator affordabilityCalculator,
            HealthScoreProperties props) {
        this.simulator = simulator;
        this.affordabilityCalculator = affordabilityCalculator;
        this.props = props;
    }

    public PurchaseDecisionDetailDto evaluate(FinancialAdvisoryContext ctx, PurchaseEvaluateDetailRequest request) {
        PurchaseDecisionDetailDto result = new PurchaseDecisionDetailDto();
        BigDecimal price = request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO;
        String label = request.getLabel() != null ? request.getLabel() : "this purchase";
        String category = request.getCategory();

        if (price.signum() <= 0) {
            result.setDecision("NOT_RECOMMENDED");
            result.setAffordabilityScore(0);
            result.setConfidence(99);
            result.setExplanation("Enter a valid purchase amount to evaluate.");
            result.setReason(result.getExplanation());
            return result;
        }

        ImpactAnalysisDto impact = simulator.simulatePurchase(ctx, price, category);
        var affordability = affordabilityCalculator.calculate(ctx, impact, price, request.getPriority());

        impact.setComponentScores(affordability.componentScores());
        result.setAffordabilityScore(affordability.totalScore());
        result.setImpactAnalysis(impact);

        String decision = mapDecision(affordability.totalScore());
        result.setDecision(decision);
        result.setConfidence(computeConfidence(affordability.totalScore(), decision));
        result.setExplanation(buildExplanation(ctx, request, impact, affordability.totalScore(), decision));
        result.setReason(result.getExplanation());

        if ("WAIT".equals(decision) || "NOT_RECOMMENDED".equals(decision)) {
            LocalDate recommended = recommendPurchaseDate(ctx, price, impact);
            result.setRecommendedPurchaseDate(recommended);
            if ("NOT_RECOMMENDED".equals(decision)) {
                BigDecimal spendable = AdvisoryMath.spendableBalance(ctx.getCurrentBalance(), ctx.getHealthContext());
                BigDecimal shortfall = price.subtract(spendable).max(BigDecimal.ZERO);
                result.setRequiredSavings(shortfall);
                result.setEstimatedDaysToAfford(estimateDaysToAfford(ctx, shortfall));
            }
        }

        result.setAlternatives(buildAlternatives(ctx, request, impact, price));
        return result;
    }

    private String mapDecision(int score) {
        var aff = props.getAffordability();
        if (score >= aff.getBuyMinScore()) return "BUY";
        if (score >= aff.getBuyWithCautionMinScore()) return "BUY WITH CAUTION";
        if (score >= aff.getWaitMinScore()) return "WAIT";
        return "NOT_RECOMMENDED";
    }

    private int computeConfidence(int score, String decision) {
        int base = switch (decision) {
            case "BUY" -> props.getDecision().getBuyConfidenceBase();
            case "BUY WITH CAUTION" -> props.getDecision().getBuyConfidenceBase() - 5;
            case "WAIT" -> props.getDecision().getWaitConfidenceBase();
            default -> props.getDecision().getNotRecommendedConfidenceBase();
        };
        int adjustment = Math.abs(score - 75) / 5;
        return Math.max(55, Math.min(99, base + adjustment));
    }

    private String buildExplanation(
            FinancialAdvisoryContext ctx,
            PurchaseEvaluateDetailRequest request,
            ImpactAnalysisDto impact,
            int score,
            String decision) {

        String label = request.getLabel() != null ? request.getLabel() : "this purchase";
        BigDecimal price = request.getPrice();
        var health = ctx.getHealthContext();
        BigDecimal rawBudget = ctx.getCategoryBudgets().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pool = AdvisoryMath.discretionaryPool(rawBudget, health, ctx.getCurrentBalance());
        double pctOfDiscretionary = pool.signum() > 0
                ? price.multiply(BigDecimal.valueOf(100))
                .divide(pool, 1, RoundingMode.HALF_UP).doubleValue()
                : (health.getMonthIncome().signum() > 0
                ? price.multiply(BigDecimal.valueOf(100))
                .divide(health.getMonthIncome(), 1, RoundingMode.HALF_UP).doubleValue()
                : 0);

        StringBuilder sb = new StringBuilder();
        sb.append(decision).append(". ");
        sb.append("Affordability score: ").append(score).append("/100. ");
        sb.append("This purchase consumes ").append(pctOfDiscretionary).append("% of your remaining monthly discretionary budget. ");

        if (impact.getEmergencyFundMonthsAfter() >= props.getAffordability().getMinEmergencyMonths()) {
            sb.append("Emergency fund remains above ")
                    .append(String.format("%.1f", impact.getEmergencyFundMonthsAfter()))
                    .append(" months. ");
        } else {
            sb.append("Emergency fund would drop to ")
                    .append(String.format("%.1f", impact.getEmergencyFundMonthsAfter()))
                    .append(" months. ");
        }

        if (impact.getSavingsGoalDelayDays() > 0) {
            sb.append("Savings goal delayed ").append(impact.getSavingsGoalDelayDays()).append(" days. ");
        }

        sb.append("Budget utilization increases ")
                .append(String.format("%.0f", impact.getBudgetUtilizationBeforePercent()))
                .append("% → ")
                .append(String.format("%.0f", impact.getBudgetUtilizationAfterPercent()))
                .append("%.");

        return sb.toString();
    }

    private LocalDate recommendPurchaseDate(FinancialAdvisoryContext ctx, BigDecimal price, ImpactAnalysisDto impact) {
        LocalDate today = ctx.getAsOfDate();
        int salaryDay = ctx.getSalaryDayOfMonth();
        LocalDate salaryDate = nextSalaryDate(today, salaryDay);

        if (ctx.getCurrentBalance().compareTo(price) < 0
                && AdvisoryMath.spendableBalance(ctx.getCurrentBalance(), ctx.getHealthContext()).compareTo(price) < 0) {
            return salaryDate;
        }
        if (impact.getEmergencyFundMonthsAfter() < props.getAffordability().getMinEmergencyMonths()) {
            long daysUntilSalary = ChronoUnit.DAYS.between(today, salaryDate);
            if (daysUntilSalary > 0) return salaryDate;
        }
        return today.plusDays(7);
    }

    private static LocalDate nextSalaryDate(LocalDate today, int salaryDay) {
        int day = Math.min(salaryDay, today.lengthOfMonth());
        LocalDate candidate = today.withDayOfMonth(day);
        if (!candidate.isAfter(today)) {
            LocalDate nextMonth = today.plusMonths(1);
            day = Math.min(salaryDay, nextMonth.lengthOfMonth());
            candidate = nextMonth.withDayOfMonth(day);
        }
        return candidate;
    }

    private int estimateDaysToAfford(FinancialAdvisoryContext ctx, BigDecimal shortfall) {
        if (shortfall.signum() <= 0) return 0;
        BigDecimal dailySavings = ctx.getHealthContext().monthSavings();
        int daysInMonth = ctx.getHealthContext().getMonth().lengthOfMonth();
        int remaining = daysInMonth - ctx.getAsOfDate().getDayOfMonth();
        if (dailySavings.signum() > 0 && remaining > 0) {
            BigDecimal daily = dailySavings.divide(BigDecimal.valueOf(Math.max(1, ctx.getAsOfDate().getDayOfMonth())), 2, RoundingMode.HALF_UP);
            if (daily.signum() > 0) {
                return shortfall.divide(daily, 0, RoundingMode.CEILING).intValue();
            }
        }
        return (int) Math.max(1, ChronoUnit.DAYS.between(ctx.getAsOfDate(), nextSalaryDate(ctx.getAsOfDate(), ctx.getSalaryDayOfMonth())));
    }

    private List<String> buildAlternatives(
            FinancialAdvisoryContext ctx,
            PurchaseEvaluateDetailRequest request,
            ImpactAnalysisDto impact,
            BigDecimal price) {

        List<String> alts = new ArrayList<>();
        if (impact.getRemainingBudgetAfter().signum() < 0) {
            alts.add("Reduce " + (request.getCategory() != null ? request.getCategory() : "discretionary")
                    + " by " + ScoreMath.inr(impact.getRemainingBudgetAfter().abs()) + " this month");
        }
        alts.add("Buy after salary on " + nextSalaryDate(ctx.getAsOfDate(), ctx.getSalaryDayOfMonth()));
        if (impact.getSavingsGoalDelayDays() > 0) {
            alts.add("Buy next month after goal contribution");
        }
        alts.add("Wait " + ChronoUnit.DAYS.between(ctx.getAsOfDate(),
                recommendPurchaseDate(ctx, price, impact)) + " days");
        return alts.stream().distinct().limit(4).toList();
    }
}
