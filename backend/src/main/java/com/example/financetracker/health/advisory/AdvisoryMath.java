package com.example.financetracker.health.advisory;

import com.example.financetracker.health.calculator.HealthScoreContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared financial math for advisory engine — works even with sparse user data. */
public final class AdvisoryMath {

    private AdvisoryMath() {}

    /** Spendable pool: budget remainder, or income minus expenses, or current balance. */
    public static BigDecimal discretionaryPool(
            BigDecimal totalBudget,
            HealthScoreContext health,
            BigDecimal currentBalance) {

        BigDecimal fromBudget = totalBudget.subtract(health.getMonthExpenses());
        BigDecimal fromIncome = health.getMonthIncome().subtract(health.getMonthExpenses());
        BigDecimal fromBalance = currentBalance != null ? currentBalance : BigDecimal.ZERO;

        BigDecimal best = fromBudget.max(fromIncome).max(fromBalance);
        return best.max(BigDecimal.ZERO);
    }

    /** Effective monthly budget cap when user has not set category budgets. */
    public static BigDecimal effectiveMonthlyBudget(BigDecimal totalBudget, HealthScoreContext health) {
        if (totalBudget.signum() > 0) return totalBudget;
        if (health.getMonthIncome().signum() > 0) return health.getMonthIncome();
        return health.getMonthExpenses().multiply(BigDecimal.valueOf(1.15)).max(BigDecimal.ONE);
    }

    /** Starting cash for purchase simulation. */
    public static BigDecimal spendableBalance(BigDecimal currentBalance, HealthScoreContext health) {
        if (currentBalance != null && currentBalance.signum() > 0) return currentBalance;
        BigDecimal monthSavings = health.getMonthIncome().subtract(health.getMonthExpenses());
        if (monthSavings.signum() > 0) return monthSavings;
        return BigDecimal.ZERO;
    }

    public static BigDecimal avgMonthlyExpense(java.util.List<BigDecimal> series, BigDecimal fallback) {
        if (series == null || series.isEmpty()) return fallback.max(BigDecimal.ONE);
        return series.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(series.size()), 2, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE);
    }
}
