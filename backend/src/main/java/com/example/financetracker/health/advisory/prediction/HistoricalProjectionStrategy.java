package com.example.financetracker.health.advisory.prediction;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

/** Projects spend using historical category averages for the remainder of the month. */
public class HistoricalProjectionStrategy implements SpendingPredictionStrategy {

    @Override
    public BigDecimal predictCategory(String category, FinancialAdvisoryContext ctx) {
        BigDecimal currentSpend = ctx.getHealthContext().getSpentByCategory()
                .getOrDefault(category, BigDecimal.ZERO);

        YearMonth month = ctx.getHealthContext().getMonth();
        int daysInMonth = month.lengthOfMonth();
        int elapsed = Math.max(1, Math.min(ctx.getAsOfDate().getDayOfMonth(), daysInMonth));
        int remaining = Math.max(0, daysInMonth - elapsed);

        List<BigDecimal> history = ctx.getHistoricalCategorySpend().getOrDefault(category, List.of());
        BigDecimal historicalAvg = BigDecimal.ZERO;
        if (!history.isEmpty()) {
            // Exclude current month partial if it's the last entry
            List<BigDecimal> prior = history.size() > 1 ? history.subList(0, history.size() - 1) : history;
            historicalAvg = prior.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(prior.size()), 4, RoundingMode.HALF_UP);
        }

        if (historicalAvg.signum() == 0) {
            return currentSpend;
        }

        BigDecimal historicalDaily = historicalAvg.divide(BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
        return currentSpend.add(historicalDaily.multiply(BigDecimal.valueOf(remaining)))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
