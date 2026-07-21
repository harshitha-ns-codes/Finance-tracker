package com.example.financetracker.health.calculator;

import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.Subscription;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubscriptionHealthCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public SubscriptionHealthCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getSubscriptionHealth();
        List<Subscription> subs = ctx.getSubscriptions();
        if (subs == null || subs.isEmpty()) {
            return new CategoryScoreDto(
                    "Subscription Health",
                    (int) Math.round(max * 0.7),
                    max,
                    "No subscriptions tracked — scored lightly. Add subscriptions to audit waste."
            );
        }

        BigDecimal total = BigDecimal.ZERO;
        int unused = 0;
        int duplicates = 0;
        List<String> waste = new ArrayList<>();
        for (Subscription s : subs) {
            total = total.add(s.getMonthlyAmount());
            if (s.isUnused()) {
                unused++;
                waste.add("Unused: " + s.getName() + " (" + ScoreMath.inr(s.getMonthlyAmount()) + "/mo)");
            }
            if (s.isDuplicate()) {
                duplicates++;
                waste.add("Duplicate: " + s.getName());
            }
        }

        BigDecimal income = ctx.getMonthIncome();
        double burdenPct = income.compareTo(BigDecimal.ZERO) > 0
                ? ScoreMath.safeDiv(total, income).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0;

        int burdenScore = ScoreMath.scalePoints(
                burdenPct,
                props.getSubscriptions().getHighBurdenIncomePercent(),
                props.getSubscriptions().getHealthyBurdenIncomePercent(),
                max
        );

        // Penalize unused/duplicates
        int wastePenalty = Math.min(max, unused + duplicates);
        int score = Math.max(0, burdenScore - wastePenalty);

        String explanation = "Subscriptions total " + ScoreMath.inr(total) + "/mo"
                + (income.compareTo(BigDecimal.ZERO) > 0 ? " (" + burdenPct + "% of income)" : "")
                + ". " + unused + " unused, " + duplicates + " duplicate.";

        CategoryScoreDto dto = new CategoryScoreDto("Subscription Health", score, max, explanation);
        dto.getDetails().add(explanation);
        dto.getDetails().addAll(waste);
        return dto;
    }
}
