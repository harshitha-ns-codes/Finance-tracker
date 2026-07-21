package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.BudgetPredictionDto;
import com.example.financetracker.health.advisory.dto.CategoryPredictionDto;
import com.example.financetracker.health.advisory.prediction.SpendingPredictionStrategy;
import com.example.financetracker.health.calculator.ScoreMath;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class PredictionService {

    private final SpendingPredictionStrategy predictionStrategy;
    private final RiskAnalysisService riskAnalysisService;

    public PredictionService(
            @Qualifier("spendingPredictionStrategy") SpendingPredictionStrategy predictionStrategy,
            RiskAnalysisService riskAnalysisService) {
        this.predictionStrategy = predictionStrategy;
        this.riskAnalysisService = riskAnalysisService;
    }

    public BudgetPredictionDto predict(FinancialAdvisoryContext ctx) {
        BudgetPredictionDto result = new BudgetPredictionDto();

        Set<String> categories = new LinkedHashSet<>(ctx.getCategoryBudgets().keySet());
        categories.addAll(ctx.getHealthContext().getSpentByCategory().keySet());

        BigDecimal totalPredicted = BigDecimal.ZERO;
        BigDecimal totalBudget = ctx.getCategoryBudgets().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int daysInMonth = ctx.getHealthContext().getMonth().lengthOfMonth();
        int elapsed = Math.max(1, Math.min(ctx.getAsOfDate().getDayOfMonth(), daysInMonth));

        for (String category : categories) {
            CategoryPredictionDto cat = predictCategory(ctx, category, elapsed, daysInMonth);
            result.getCategories().add(cat);
            totalPredicted = totalPredicted.add(cat.getPredictedSpend());
        }

        result.setExpectedMonthlySpend(totalPredicted);
        result.setTotalBudget(totalBudget);

        BigDecimal income = ctx.getHealthContext().getMonthIncome();
        result.setExpectedSavings(income.subtract(totalPredicted));

        double overallRisk = riskAnalysisService.riskScorePercent(
                totalPredicted.doubleValue(),
                totalBudget.max(BigDecimal.ONE).doubleValue());
        result.setOverspendProbability(riskAnalysisService.overspendProbability(overallRisk));
        result.setRiskLevel(riskAnalysisService.riskLevel(overallRisk));

        return result;
    }

    private CategoryPredictionDto predictCategory(
            FinancialAdvisoryContext ctx, String category, int elapsed, int daysInMonth) {
        CategoryPredictionDto dto = new CategoryPredictionDto();
        dto.setCategory(category);

        BigDecimal budget = ctx.getCategoryBudgets().getOrDefault(category, BigDecimal.ZERO);
        BigDecimal current = ctx.getHealthContext().getSpentByCategory()
                .getOrDefault(category, BigDecimal.ZERO);
        BigDecimal predicted = predictionStrategy.predictCategory(category, ctx);

        dto.setBudget(budget);
        dto.setCurrentSpend(current);
        dto.setPredictedSpend(predicted);

        BigDecimal overspend = predicted.subtract(budget).max(BigDecimal.ZERO);
        dto.setOverspendAmount(overspend);

        double riskPct = riskAnalysisService.riskScorePercent(
                predicted.doubleValue(), budget.max(BigDecimal.ONE).doubleValue());
        dto.setRiskScorePercent(Math.round(riskPct * 10) / 10.0);
        dto.setRiskLevel(riskAnalysisService.riskLevel(riskPct));

        BigDecimal dailyRate = current.divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
        dto.setDailySpendRate(dailyRate);

        if (overspend.signum() > 0 && elapsed < daysInMonth) {
            int remaining = daysInMonth - elapsed;
            BigDecimal dailyCut = overspend.divide(BigDecimal.valueOf(Math.max(1, remaining)), 0, RoundingMode.CEILING);
            dto.setRecommendation("Reduce " + category + " spending by "
                    + ScoreMath.inr(dailyCut) + "/day to remain within budget.");
        } else if (riskPct >= 90 && budget.signum() > 0) {
            dto.setRecommendation("Monitor " + category + " closely — projected at "
                    + Math.round(riskPct) + "% of budget.");
        }

        return dto;
    }
}
