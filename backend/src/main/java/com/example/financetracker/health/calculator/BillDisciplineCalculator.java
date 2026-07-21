package com.example.financetracker.health.calculator;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.config.HealthScoreProperties;
import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BillDisciplineCalculator implements ScoreComponent {

    private final HealthScoreProperties props;

    public BillDisciplineCalculator(HealthScoreProperties props) {
        this.props = props;
    }

    @Override
    public CategoryScoreDto calculate(HealthScoreContext ctx) {
        int max = props.getWeights().getBillDiscipline();
        List<CategoryBudgetItem> bills = ctx.getBudgetItems().stream()
                .filter(CategoryBudgetItem::isFixed)
                .toList();

        // Also treat unpaid budget lines with due dates as bills when marked for payment tracking
        if (bills.isEmpty()) {
            bills = ctx.getBudgetItems().stream()
                    .filter(i -> i.getDueDate() != null)
                    .toList();
        }

        if (bills.isEmpty()) {
            return new CategoryScoreDto(
                    "Bill Payment Discipline",
                    (int) Math.round(max * 0.7),
                    max,
                    "No recurring/fixed bills tracked this month — scored lightly. Mark budget lines as bills via due dates or fixed flags."
            );
        }

        int paid = 0;
        int unpaid = 0;
        List<String> unpaidNames = new ArrayList<>();
        for (CategoryBudgetItem bill : bills) {
            if (bill.isPaid()) {
                paid++;
            } else {
                unpaid++;
                unpaidNames.add(bill.getDescription() != null && !bill.getDescription().isBlank()
                        ? bill.getDescription()
                        : bill.getCategory());
            }
        }

        double ratio = (double) paid / bills.size();
        int score = (int) Math.round(ratio * max);

        String explanation;
        if (unpaid == 0) {
            explanation = "Paid all " + bills.size() + " tracked bills on time this month.";
        } else {
            explanation = unpaid + " of " + bills.size() + " bills still unpaid"
                    + (unpaidNames.isEmpty() ? "." : ": " + String.join(", ", unpaidNames) + ".");
        }

        CategoryScoreDto dto = new CategoryScoreDto("Bill Payment Discipline", score, max, explanation);
        dto.getDetails().add(explanation);
        return dto;
    }
}
