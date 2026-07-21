package com.example.financetracker.health.calculator;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BudgetAdherenceCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public BudgetAdherenceCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getBudgetAdherence();
        List<CategoryBudgetItem> items = ctx.getBudgetItems();
        if (items == null || items.isEmpty()) {
            CategoryScoreDto dto = new CategoryScoreDto(
                    "Budget Adherence",
                    (int) Math.round(max * 0.5),
                    max,
                    "No category budgets set for this month — scored neutrally. Add budgets to unlock full scoring."
            );
            dto.getDetails().add("Set category budgets to measure adherence accurately.");
            return dto;
        }

        Map<String, BigDecimal> plannedByCat = new LinkedHashMap<>();
        for (CategoryBudgetItem item : items) {
            plannedByCat.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }

        BigDecimal totalPlanned = BigDecimal.ZERO;
        BigDecimal weightedPenalty = BigDecimal.ZERO;
        List<String> overs = new ArrayList<>();
        List<String> goods = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> e : plannedByCat.entrySet()) {
            String cat = e.getKey();
            BigDecimal planned = e.getValue();
            BigDecimal spent = ctx.getSpentByCategory().getOrDefault(cat, BigDecimal.ZERO);
            totalPlanned = totalPlanned.add(planned);

            if (planned.compareTo(BigDecimal.ZERO) <= 0) continue;

            if (spent.compareTo(planned) <= 0) {
                goods.add("Stayed within " + cat + " budget (" + ScoreMath.inr(spent) + " of " + ScoreMath.inr(planned) + ")");
            } else {
                BigDecimal over = spent.subtract(planned);
                BigDecimal overRatio = over.divide(planned, 4, RoundingMode.HALF_UP);
                weightedPenalty = weightedPenalty.add(overRatio.multiply(planned));
                overs.add(cat + " exceeded budget by " + ScoreMath.inr(over));
            }
        }

        double adherence;
        if (totalPlanned.compareTo(BigDecimal.ZERO) == 0) {
            adherence = 0.5;
        } else {
            // penalty is sum(over/planned * planned) / totalPlanned = totalOverspend / totalPlanned
            double penaltyRatio = ScoreMath.safeDiv(weightedPenalty, totalPlanned).doubleValue();
            adherence = Math.max(0, 1.0 - penaltyRatio);
        }

        int score = (int) Math.round(adherence * max);
        String explanation;
        if (overs.isEmpty()) {
            explanation = "All category budgets are within plan this month.";
        } else {
            explanation = String.join(" ", overs);
        }

        CategoryScoreDto dto = new CategoryScoreDto("Budget Adherence", score, max, explanation);
        dto.getDetails().addAll(goods);
        dto.getDetails().addAll(overs);
        return dto;
    }
}
