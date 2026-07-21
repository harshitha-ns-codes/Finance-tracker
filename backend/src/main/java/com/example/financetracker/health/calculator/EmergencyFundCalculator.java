package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.FinancialProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class EmergencyFundCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public EmergencyFundCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getEmergencyFund();
        FinancialProfile profile = ctx.getProfile();
        BigDecimal emergency = profile != null ? profile.getEmergencyFundBalance() : BigDecimal.ZERO;

        BigDecimal avgMonthlyExpense = averageExpenses(ctx.getMonthlyExpenseSeries(), ctx.getMonthExpenses());
        if (avgMonthlyExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return new CategoryScoreDto(
                    "Emergency Fund",
                    (int) Math.round(max * 0.5),
                    max,
                    "Not enough expense history to measure coverage. Keep logging expenses."
            );
        }

        double months = ScoreMath.safeDiv(emergency, avgMonthlyExpense)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        int score = ScoreMath.scalePoints(
                months,
                props.getEmergencyFund().getZeroScoreMonths(),
                props.getEmergencyFund().getFullScoreMonths(),
                max
        );

        String explanation = "Emergency fund covers approximately " + months + " months of expenses ("
                + ScoreMath.inr(emergency) + " / " + ScoreMath.inr(avgMonthlyExpense) + " avg monthly).";
        CategoryScoreDto dto = new CategoryScoreDto("Emergency Fund", score, max, explanation);
        dto.getDetails().add(explanation);
        if (emergency.compareTo(BigDecimal.ZERO) == 0) {
            dto.getDetails().add("No emergency fund balance recorded yet.");
        }
        return dto;
    }

    private static BigDecimal averageExpenses(List<BigDecimal> series, BigDecimal fallback) {
        if (series == null || series.isEmpty()) return fallback;
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (BigDecimal v : series) {
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(v);
                n++;
            }
        }
        if (n == 0) return fallback;
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }
}
