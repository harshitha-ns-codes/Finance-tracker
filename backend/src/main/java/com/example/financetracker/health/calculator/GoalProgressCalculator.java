package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.SavingsGoal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoalProgressCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public GoalProgressCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getGoalProgress();
        List<SavingsGoal> goals = ctx.getGoals();
        if (goals == null || goals.isEmpty()) {
            return new CategoryScoreDto(
                    "Goal Progress",
                    (int) Math.round(max * 0.5),
                    max,
                    "No active savings goals — scored neutrally. Add a goal to unlock this component."
            );
        }

        int totalPoints = 0;
        List<String> notes = new ArrayList<>();
        for (SavingsGoal goal : goals) {
            int component = scoreOne(goal, ctx.monthSavings(), notes);
            totalPoints += component;
        }
        int score = Math.round((float) totalPoints / goals.size());
        score = Math.max(0, Math.min(max, score));

        String explanation = notes.isEmpty()
                ? "All savings goals are on track."
                : String.join(" ", notes);
        CategoryScoreDto dto = new CategoryScoreDto("Goal Progress", score, max, explanation);
        dto.getDetails().addAll(notes);
        return dto;
    }

    private int scoreOne(SavingsGoal goal, BigDecimal monthSavings, List<String> notes) {
        int max = props.getWeights().getGoalProgress();
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
            notes.add(goal.getName() + " is fully funded.");
            return max;
        }

        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
        if (daysLeft <= 0) {
            notes.add(goal.getName() + " is past its deadline with " + ScoreMath.inr(remaining) + " remaining.");
            return 0;
        }

        long monthsLeft = Math.max(1, (daysLeft + 29) / 30);
        BigDecimal expectedMonthly = remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP);

        if (monthSavings.compareTo(expectedMonthly) >= 0) {
            notes.add(goal.getName() + " is on schedule (need ~" + ScoreMath.inr(expectedMonthly) + "/mo).");
            return max;
        }

        double ratio = ScoreMath.safeDiv(monthSavings.max(BigDecimal.ZERO), expectedMonthly).doubleValue();
        int pts = (int) Math.round(Math.max(0, Math.min(1, ratio)) * max);
        BigDecimal shortfall = expectedMonthly.subtract(monthSavings).max(BigDecimal.ZERO);
        notes.add(goal.getName() + " is behind — contribute ~" + ScoreMath.inr(shortfall)
                + " more this month to stay on track.");
        return pts;
    }
}
