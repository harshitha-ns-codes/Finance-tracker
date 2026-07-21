package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.FinancialProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DebtHealthCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public DebtHealthCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getDebtHealth();
        FinancialProfile profile = ctx.getProfile();
        BigDecimal debtPayment = profile != null ? profile.getMonthlyDebtPayments() : BigDecimal.ZERO;
        BigDecimal income = ctx.getMonthIncome();

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return new CategoryScoreDto(
                    "Debt Health",
                    (int) Math.round(max * 0.5),
                    max,
                    "No income this month to compute debt-to-income ratio."
            );
        }

        double dti = ScoreMath.safeDiv(debtPayment, income)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        // Lower DTI is better
        int score = ScoreMath.scalePoints(
                dti,
                props.getDebt().getZeroScoreDtiPercent(),
                props.getDebt().getFullScoreDtiPercent(),
                max
        );

        String explanation = debtPayment.compareTo(BigDecimal.ZERO) == 0
                ? "No monthly debt payments recorded — strong debt health signal."
                : "Debt-to-income (monthly payments) is " + dti + "%.";
        CategoryScoreDto dto = new CategoryScoreDto("Debt Health", score, max, explanation);
        dto.getDetails().add(explanation);
        return dto;
    }
}
