package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SpendingConsistencyCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public SpendingConsistencyCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getSpendingConsistency();
        List<BigDecimal> series = ctx.getMonthlyExpenseSeries();
        if (series == null || series.size() < 2) {
            return new CategoryScoreDto(
                    "Spending Consistency",
                    (int) Math.round(max * 0.6),
                    max,
                    "Need at least 2 months of expenses to measure consistency."
            );
        }

        double mean = series.stream().mapToDouble(ScoreMath::asDouble).average().orElse(0);
        if (mean <= 0) {
            return new CategoryScoreDto("Spending Consistency", max / 2, max,
                    "Average monthly spend is zero — limited consistency signal.");
        }

        double variance = 0;
        for (BigDecimal v : series) {
            double d = ScoreMath.asDouble(v) - mean;
            variance += d * d;
        }
        variance /= series.size();
        double std = Math.sqrt(variance);
        double cvPercent = (std / mean) * 100.0;

        // Lower CV is better
        int score = ScoreMath.scalePoints(
                cvPercent,
                props.getSpendingConsistency().getMaxCvPercent(),
                props.getSpendingConsistency().getIdealCvPercent(),
                max
        );

        // Detect spike: last month vs previous
        double last = ScoreMath.asDouble(series.get(series.size() - 1));
        double prev = ScoreMath.asDouble(series.get(series.size() - 2));
        String spikeNote = "";
        if (prev > 0 && last > prev * 1.15) {
            double up = ((last - prev) / prev) * 100.0;
            spikeNote = " Spending rose " + BigDecimal.valueOf(up).setScale(0, RoundingMode.HALF_UP) + "% vs prior month.";
            score = Math.max(0, score - Math.max(1, max / 5));
        }

        String explanation = "Month-to-month spend variation (CV) is "
                + BigDecimal.valueOf(cvPercent).setScale(1, RoundingMode.HALF_UP) + "%."
                + spikeNote;
        CategoryScoreDto dto = new CategoryScoreDto("Spending Consistency", score, max, explanation.trim());
        dto.getDetails().add(explanation.trim());
        return dto;
    }
}
