package com.example.financetracker.health.advisory.prediction;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.config.HealthScoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Blends pace and historical projections with configurable weights. */
public class WeightedCompositePredictionStrategy implements SpendingPredictionStrategy {

    private final PaceProjectionStrategy paceStrategy;
    private final HistoricalProjectionStrategy historicalStrategy;
    private final HealthScoreProperties props;

    public WeightedCompositePredictionStrategy(
            PaceProjectionStrategy paceStrategy,
            HistoricalProjectionStrategy historicalStrategy,
            HealthScoreProperties props) {
        this.paceStrategy = paceStrategy;
        this.historicalStrategy = historicalStrategy;
        this.props = props;
    }

    @Override
    public BigDecimal predictCategory(String category, FinancialAdvisoryContext ctx) {
        BigDecimal pace = paceStrategy.predictCategory(category, ctx);
        BigDecimal historical = historicalStrategy.predictCategory(category, ctx);

        double wCurrent = props.getPrediction().getCurrentMonthWeight();
        double wHistorical = props.getPrediction().getHistoricalWeight();
        double total = wCurrent + wHistorical;
        if (total <= 0) {
            return pace;
        }
        wCurrent /= total;
        wHistorical /= total;

        return pace.multiply(BigDecimal.valueOf(wCurrent))
                .add(historical.multiply(BigDecimal.valueOf(wHistorical)))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
