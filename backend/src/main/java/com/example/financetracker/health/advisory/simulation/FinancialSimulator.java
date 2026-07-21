package com.example.financetracker.health.advisory.simulation;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.advisory.AdvisoryMath;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.BudgetPredictionDto;
import com.example.financetracker.health.advisory.dto.ImpactAnalysisDto;
import com.example.financetracker.health.advisory.service.PredictionService;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.SavingsGoal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Deterministic financial impact simulation for a hypothetical purchase. */
@Component
public class FinancialSimulator {

    private final PredictionService predictionService;

    public FinancialSimulator(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    public ImpactAnalysisDto simulatePurchase(
            FinancialAdvisoryContext ctx, BigDecimal price, String category) {
        ImpactAnalysisDto impact = new ImpactAnalysisDto();
        var health = ctx.getHealthContext();

        BigDecimal totalBudget = AdvisoryMath.effectiveMonthlyBudget(
                ctx.getCategoryBudgets().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                health);
        BudgetPredictionDto prediction = predictionService.predict(ctx);
        BigDecimal predictedSpend = prediction.getExpectedMonthlySpend();

        BigDecimal spendable = AdvisoryMath.spendableBalance(ctx.getCurrentBalance(), health);
        BigDecimal balanceAfter = spendable.subtract(price);
        impact.setBalanceAfterPurchase(balanceAfter);

        BigDecimal pool = AdvisoryMath.discretionaryPool(
                ctx.getCategoryBudgets().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                health, ctx.getCurrentBalance());
        impact.setRemainingBudgetAfter(pool.subtract(price));

        FinancialProfile profile = health.getProfile();
        BigDecimal emergency = profile != null ? profile.getEmergencyFundBalance() : BigDecimal.ZERO;
        BigDecimal avgExpense = AdvisoryMath.avgMonthlyExpense(
                health.getMonthlyExpenseSeries(), health.getMonthExpenses());
        double monthsAfter = ScoreMath.safeDiv(emergency.subtract(price).max(BigDecimal.ZERO), avgExpense).doubleValue();
        impact.setEmergencyFundMonthsAfter(Math.round(monthsAfter * 10) / 10.0);

        impact.setSavingsGoalDelayDays(estimateGoalDelayDays(health.getGoals(), price));

        BigDecimal unpaidBills = health.getBudgetItems().stream()
                .filter(i -> (i.isFixed() || i.getDueDate() != null) && !i.isPaid())
                .map(CategoryBudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        impact.setUpcomingBillsCovered(balanceAfter.compareTo(unpaidBills) >= 0);

        BigDecimal predictedWithPurchase = predictedSpend.add(price);
        impact.setExpectedEndOfMonthBalance(
                health.getMonthIncome().subtract(predictedWithPurchase).add(balanceAfter.max(BigDecimal.ZERO)));

        if (totalBudget.signum() > 0) {
            impact.setBudgetUtilizationBeforePercent(
                    predictedSpend.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudget, 1, RoundingMode.HALF_UP).doubleValue());
            impact.setBudgetUtilizationAfterPercent(
                    predictedWithPurchase.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudget, 1, RoundingMode.HALF_UP).doubleValue());
        }

        return impact;
    }

    private static int estimateGoalDelayDays(java.util.List<SavingsGoal> goals, BigDecimal price) {
        for (SavingsGoal goal : goals) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            if (remaining.signum() <= 0) continue;
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (daysLeft <= 0) continue;
            BigDecimal dailyNeed = remaining.divide(BigDecimal.valueOf(Math.max(1, daysLeft)), 4, RoundingMode.HALF_UP);
            if (dailyNeed.signum() > 0) {
                return price.divide(dailyNeed, 0, RoundingMode.CEILING).intValue();
            }
        }
        return 0;
    }
}
