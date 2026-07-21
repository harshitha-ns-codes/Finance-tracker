package com.example.financetracker.health.decision;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.PurchaseDecisionResponse;
import com.example.financetracker.health.model.SavingsGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseDecisionEngine {

    private final HealthScoreProperties props;

    public PurchaseDecisionEngine(HealthScoreProperties props) {
        this.props = props;
    }

    public PurchaseDecisionResponse evaluate(HealthScoreContext ctx, BigDecimal price, String label) {
        PurchaseDecisionResponse res = new PurchaseDecisionResponse();
        List<String> alternatives = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        if (price == null || price.signum() <= 0) {
            res.setDecision("NOT_RECOMMENDED");
            res.setConfidence(props.getDecision().getNotRecommendedConfidenceBase());
            res.setReason("Enter a valid purchase amount to evaluate.");
            return res;
        }

        BigDecimal totalBudget = ctx.getBudgetItems().stream()
                .map(CategoryBudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingBudget = totalBudget.subtract(ctx.getMonthExpenses());
        BigDecimal unpaidBills = ctx.getBudgetItems().stream()
                .filter(i -> (i.isFixed() || i.getDueDate() != null) && !i.isPaid())
                .map(CategoryBudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal emergency = ctx.getProfile() != null
                ? ctx.getProfile().getEmergencyFundBalance() : BigDecimal.ZERO;
        BigDecimal avgExpense = ctx.getMonthExpenses().max(BigDecimal.ONE);
        if (!ctx.getMonthlyExpenseSeries().isEmpty()) {
            avgExpense = ctx.getMonthlyExpenseSeries().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(1, ctx.getMonthlyExpenseSeries().size())), 2, RoundingMode.HALF_UP)
                    .max(BigDecimal.ONE);
        }
        double monthsCover = ScoreMath.safeDiv(emergency, avgExpense).doubleValue();
        double monthsAfter = ScoreMath.safeDiv(emergency.subtract(price).max(BigDecimal.ZERO), avgExpense).doubleValue();

        // Predicted month-end spend
        int day = LocalDate.now().getDayOfMonth();
        int daysInMonth = ctx.getMonth().lengthOfMonth();
        double pace = day <= 0 ? 1 : (double) daysInMonth / day;
        BigDecimal predictedMonthEnd = ctx.getMonthExpenses().multiply(BigDecimal.valueOf(pace))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal predictedWithPurchase = predictedMonthEnd.add(price);

        // Goal delay estimate using nearest active goal
        String goalImpact = null;
        for (SavingsGoal goal : ctx.getGoals()) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            if (remaining.signum() <= 0) continue;
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (daysLeft <= 0) continue;
            BigDecimal dailyNeed = remaining.divide(BigDecimal.valueOf(Math.max(1, daysLeft)), 4, RoundingMode.HALF_UP);
            if (dailyNeed.signum() > 0) {
                int delayDays = price.divide(dailyNeed, 0, RoundingMode.CEILING).intValue();
                goalImpact = "delay your \"" + goal.getName() + "\" goal by about " + delayDays + " days";
                break;
            }
        }

        boolean blowsBudget = remainingBudget.compareTo(price) < 0;
        boolean unpaidPressure = unpaidBills.signum() > 0 && price.compareTo(unpaidBills) > 0;
        boolean emergencyRisk = monthsAfter < props.getDecision().getMinEmergencyMonths()
                && monthsCover >= props.getDecision().getMinEmergencyMonths();
        boolean emergencyAlreadyLow = monthsCover < props.getDecision().getMinEmergencyMonths();
        boolean overPace = totalBudget.signum() > 0 && predictedWithPurchase.compareTo(totalBudget) > 0;

        if (blowsBudget) {
            risks.add("this exceeds remaining monthly budget by "
                    + ScoreMath.inr(price.subtract(remainingBudget.max(BigDecimal.ZERO))));
            alternatives.add("Reduce shopping by " + ScoreMath.inr(price.subtract(remainingBudget.max(BigDecimal.ZERO)))
                    + " this month");
        }
        if (unpaidPressure) {
            risks.add("you still have " + ScoreMath.inr(unpaidBills) + " in unpaid bills");
            alternatives.add("Pay outstanding bills first");
        }
        if (emergencyRisk || (emergencyAlreadyLow && price.compareTo(avgExpense.multiply(BigDecimal.valueOf(0.1))) > 0)) {
            risks.add("it would leave emergency cover at ~"
                    + BigDecimal.valueOf(monthsAfter).setScale(1, RoundingMode.HALF_UP)
                    + " months (target "
                    + props.getDecision().getMinEmergencyMonths() + ")");
            alternatives.add("Buy after salary");
        }
        if (goalImpact != null && (blowsBudget || emergencyRisk || overPace)) {
            risks.add(goalImpact);
            alternatives.add("Buy next month after goal contribution");
        }
        if (overPace && !blowsBudget) {
            risks.add("projected month-end spend would be " + ScoreMath.inr(predictedWithPurchase)
                    + " vs budget " + ScoreMath.inr(totalBudget));
            alternatives.add("Wait until mid-month spend is clearer");
        }

        String item = (label == null || label.isBlank()) ? "this purchase" : label;

        if (risks.isEmpty() && remainingBudget.compareTo(price) >= 0 && !emergencyAlreadyLow) {
            res.setDecision("BUY");
            res.setConfidence(clamp(props.getDecision().getBuyConfidenceBase()
                    + (int) Math.min(20, remainingBudget.subtract(price)
                    .divide(price.max(BigDecimal.ONE), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.TEN).intValue())));
            res.setReason("You can afford " + item + " (" + ScoreMath.inr(price)
                    + ") without breaking budget or emergency targets. Remaining budget after purchase: "
                    + ScoreMath.inr(remainingBudget.subtract(price)) + ".");
            alternatives.add("Still fine to wait a week if you want extra cushion");
        } else if (blowsBudget && emergencyAlreadyLow) {
            res.setDecision("NOT_RECOMMENDED");
            res.setConfidence(props.getDecision().getNotRecommendedConfidenceBase());
            res.setReason("Buying " + item + " today is not recommended: "
                    + String.join("; ", risks) + ".");
            if (alternatives.isEmpty()) {
                alternatives.add("Buy after salary");
                alternatives.add("Buy next month");
            }
        } else {
            res.setDecision("WAIT");
            res.setConfidence(props.getDecision().getWaitConfidenceBase());
            res.setReason("Buying " + item + " today will "
                    + (risks.isEmpty() ? "pressure your plan." : String.join(" and ", risks) + "."));
            if (alternatives.isEmpty()) {
                alternatives.add("Buy after salary");
                alternatives.add("Buy next month");
            }
        }

        res.setAlternatives(alternatives.stream().distinct().limit(4).toList());
        return res;
    }

    private static int clamp(int v) {
        return Math.max(50, Math.min(99, v));
    }
}
