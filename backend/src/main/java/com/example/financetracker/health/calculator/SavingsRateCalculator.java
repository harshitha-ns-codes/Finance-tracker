package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SavingsRateCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public SavingsRateCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getSavingsRate();
        BigDecimal income = ctx.getMonthIncome();
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return new CategoryScoreDto(
                    "Savings Rate",
                    (int) Math.round(max * 0.4),
                    max,
                    "No income recorded this month — scored cautiously. Log income transactions to measure savings rate."
            );
        }

        BigDecimal savings = ctx.monthSavings();
        double ratePct = ScoreMath.safeDiv(savings, income)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        int score = ScoreMath.scalePoints(
                ratePct,
                props.getSavingsRate().getZeroScorePercent(),
                props.getSavingsRate().getFullScorePercent(),
                max
        );

        String explanation = "Savings rate is " + ratePct + "% ("
                + ScoreMath.inr(savings) + " saved from " + ScoreMath.inr(income) + " income).";
        CategoryScoreDto dto = new CategoryScoreDto("Savings Rate", score, max, explanation);
        dto.getDetails().add(explanation);
        return dto;
    }
}
