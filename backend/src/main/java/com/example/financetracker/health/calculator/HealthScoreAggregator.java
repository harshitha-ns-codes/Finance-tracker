package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HealthScoreAggregator {

    private final List<ScoreComponent> components;
    private final HealthScoreProperties props;

    public HealthScoreAggregator(
            BudgetAdherenceCalculator budgetAdherenceCalculator,
            SavingsRateCalculator savingsRateCalculator,
            EmergencyFundCalculator emergencyFundCalculator,
            BillDisciplineCalculator billDisciplineCalculator,
            SpendingConsistencyCalculator spendingConsistencyCalculator,
            GoalProgressCalculator goalProgressCalculator,
            DebtHealthCalculator debtHealthCalculator,
            SubscriptionHealthCalculator subscriptionHealthCalculator,
            HealthScoreProperties props) {
        this.props = props;
        this.components = List.of(
                budgetAdherenceCalculator,
                savingsRateCalculator,
                emergencyFundCalculator,
                billDisciplineCalculator,
                spendingConsistencyCalculator,
                goalProgressCalculator,
                debtHealthCalculator,
                subscriptionHealthCalculator
        );
    }

    public List<CategoryScoreDto> calculateAll(HealthScoreContext ctx) {
        List<CategoryScoreDto> breakdown = new ArrayList<>();
        for (ScoreComponent component : components) {
            breakdown.add(component.calculate(ctx));
        }
        return breakdown;
    }

    public int totalScore(List<CategoryScoreDto> breakdown) {
        int configuredTotal = props.getWeights().total();
        int raw = breakdown.stream().mapToInt(CategoryScoreDto::getScore).sum();
        if (configuredTotal <= 0) return 0;
        return (int) Math.round(raw * (100.0 / configuredTotal));
    }

    public String rating(int score) {
        var r = props.getRatings();
        if (score >= r.getExcellentMin()) return "Excellent";
        if (score >= r.getGoodMin()) return "Good";
        if (score >= r.getFairMin()) return "Fair";
        return "Needs Attention";
    }
}
