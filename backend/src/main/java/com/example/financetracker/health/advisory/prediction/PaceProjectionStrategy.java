package com.example.financetracker.health.advisory.prediction;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/** Projects spend from current daily rate through month-end. */
public class PaceProjectionStrategy implements SpendingPredictionStrategy {

    @Override
    public BigDecimal predictCategory(String category, FinancialAdvisoryContext ctx) {
        BigDecimal currentSpend = ctx.getHealthContext().getSpentByCategory()
                .getOrDefault(category, BigDecimal.ZERO);

        YearMonth month = ctx.getHealthContext().getMonth();
        int daysInMonth = month.lengthOfMonth();
        int elapsed = Math.max(1, Math.min(ctx.getAsOfDate().getDayOfMonth(), daysInMonth));
        int remaining = Math.max(0, daysInMonth - elapsed);

        BigDecimal dailyRate = currentSpend.divide(BigDecimal.valueOf(elapsed), 4, RoundingMode.HALF_UP);
        return currentSpend.add(dailyRate.multiply(BigDecimal.valueOf(remaining)))
                .setScale(0, RoundingMode.HALF_UP);
    }
}
