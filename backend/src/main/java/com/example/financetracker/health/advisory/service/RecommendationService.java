package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.BudgetPredictionDto;
import com.example.financetracker.health.advisory.dto.CategoryPredictionDto;
import com.example.financetracker.health.calculator.ScoreMath;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    public List<String> generate(FinancialAdvisoryContext ctx, BudgetPredictionDto prediction) {
        List<String> recs = new ArrayList<>();

        for (CategoryPredictionDto cat : prediction.getCategories()) {
            if (cat.getRecommendation() != null && !cat.getRecommendation().isBlank()) {
                recs.add(cat.getRecommendation());
            }
            if (cat.getOverspendAmount() != null && cat.getOverspendAmount().signum() > 0) {
                recs.add("Cut " + cat.getCategory() + " by "
                        + ScoreMath.inr(cat.getOverspendAmount()) + " this month to avoid overspend.");
            }
        }

        if ("CRITICAL".equals(prediction.getRiskLevel()) || "HIGH".equals(prediction.getRiskLevel())) {
            recs.add("Overall month-end spend projected at "
                    + ScoreMath.inr(prediction.getExpectedMonthlySpend())
                    + " vs budget " + ScoreMath.inr(prediction.getTotalBudget())
                    + " (" + prediction.getRiskLevel() + " risk).");
        }

        if (prediction.getExpectedSavings() != null && prediction.getExpectedSavings().signum() < 0) {
            recs.add("Projected deficit of " + ScoreMath.inr(prediction.getExpectedSavings().abs())
                    + " — reduce discretionary spending or defer non-essential purchases.");
        }

        if (recs.isEmpty()) {
            recs.add("Spending pace looks healthy — maintain current habits.");
        }
        return recs.stream().distinct().limit(8).toList();
    }
}
