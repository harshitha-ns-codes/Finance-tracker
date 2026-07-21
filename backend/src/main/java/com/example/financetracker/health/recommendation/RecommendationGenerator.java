package com.example.financetracker.health.recommendation;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.model.Subscription;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationGenerator {

    private final HealthScoreProperties props;

    public RecommendationGenerator(HealthScoreProperties props) {
        this.props = props;
    }

    public List<String> recommendations(HealthScoreContext ctx, List<CategoryScoreDto> breakdown) {
        List<String> recs = new ArrayList<>();

        // Budget overspend cuts
        Map<String, BigDecimal> spent = ctx.getSpentByCategory();
        for (CategoryBudgetItem item : ctx.getBudgetItems()) {
            // aggregate later — use spent map vs planned per category once
        }
        Map<String, BigDecimal> planned = new java.util.LinkedHashMap<>();
        for (CategoryBudgetItem item : ctx.getBudgetItems()) {
            planned.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : planned.entrySet()) {
            BigDecimal s = spent.getOrDefault(e.getKey(), BigDecimal.ZERO);
            if (s.compareTo(e.getValue()) > 0) {
                BigDecimal cut = s.subtract(e.getValue());
                recs.add("Reduce " + e.getKey() + " by " + ScoreMath.inr(cut) + " to get back on budget.");
            }
        }

        // Emergency fund
        BigDecimal emergency = ctx.getProfile() != null
                ? ctx.getProfile().getEmergencyFundBalance() : BigDecimal.ZERO;
        BigDecimal avgExpense = ctx.getMonthExpenses().max(BigDecimal.ONE);
        if (!ctx.getMonthlyExpenseSeries().isEmpty()) {
            avgExpense = ctx.getMonthlyExpenseSeries().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(ctx.getMonthlyExpenseSeries().size()), 2, RoundingMode.HALF_UP)
                    .max(BigDecimal.ONE);
        }
        boolean hasExpenseSignal = ctx.getMonthExpenses().compareTo(BigDecimal.ZERO) > 0
                || !ctx.getMonthlyExpenseSeries().isEmpty();
        double months = ScoreMath.safeDiv(emergency, avgExpense).doubleValue();
        if (hasExpenseSignal && months < props.getDecision().getMinEmergencyMonths()) {
            BigDecimal need = avgExpense.multiply(BigDecimal.valueOf(props.getDecision().getMinEmergencyMonths()))
                    .subtract(emergency).max(BigDecimal.ZERO);
            if (need.compareTo(BigDecimal.valueOf(100)) > 0) {
                recs.add("Increase emergency fund by " + ScoreMath.inr(need)
                        + " to reach " + props.getDecision().getMinEmergencyMonths() + " months of cover.");
            }
        }

        // Goals
        for (SavingsGoal goal : ctx.getGoals()) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (remaining.signum() > 0 && days > 0) {
                long monthsLeft = Math.max(1, (days + 29) / 30);
                BigDecimal expected = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP);
                if (ctx.monthSavings().compareTo(expected) < 0) {
                    recs.add("Contribute " + ScoreMath.inr(expected) + " toward \"" + goal.getName() + "\" this month.");
                }
            }
        }

        // Subscriptions
        for (Subscription s : ctx.getSubscriptions()) {
            if (s.isUnused()) {
                recs.add("Cancel unused " + s.getName() + " (" + ScoreMath.inr(s.getMonthlyAmount()) + "/mo).");
            } else if (s.isDuplicate()) {
                recs.add("Remove duplicate subscription: " + s.getName() + ".");
            }
        }

        // Unpaid bills
        for (CategoryBudgetItem item : ctx.getBudgetItems()) {
            if ((item.isFixed() || item.getDueDate() != null) && !item.isPaid()) {
                String name = item.getDescription() != null && !item.getDescription().isBlank()
                        ? item.getDescription() : item.getCategory();
                recs.add("Pay outstanding bill: " + name + " (" + ScoreMath.inr(item.getPlannedAmount()) + ").");
            }
        }

        // Weak components generic
        for (CategoryScoreDto cat : breakdown) {
            if (cat.getMax() > 0 && (double) cat.getScore() / cat.getMax() < 0.5) {
                String tip = "Improve " + cat.getCategory() + " — currently "
                        + cat.getScore() + "/" + cat.getMax() + ".";
                if (recs.stream().noneMatch(r -> r.contains(cat.getCategory()))) {
                    recs.add(tip);
                }
            }
        }

        if (recs.isEmpty()) {
            recs.add("Maintain current habits — your finances look solid this month.");
        }
        return recs.stream().distinct().limit(8).toList();
    }
}
