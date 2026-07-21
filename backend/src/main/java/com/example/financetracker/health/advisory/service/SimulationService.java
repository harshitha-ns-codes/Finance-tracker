package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.dto.*;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.calculator.ScoreMath;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.SavingsGoal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationService {

    private final PredictionService predictionService;
    private final PurchaseDecisionService purchaseDecisionService;
    private final HealthScoreAggregator healthScoreAggregator;

    public SimulationService(
            PredictionService predictionService,
            PurchaseDecisionService purchaseDecisionService,
            HealthScoreAggregator healthScoreAggregator) {
        this.predictionService = predictionService;
        this.purchaseDecisionService = purchaseDecisionService;
        this.healthScoreAggregator = healthScoreAggregator;
    }

    public List<SimulationResultDto> runScenarios(
            FinancialAdvisoryContext baseCtx, List<SimulationRequest> scenarios) {

        List<SimulationResultDto> results = new ArrayList<>();
        for (SimulationRequest scenario : scenarios) {
            results.add(runScenario(baseCtx, scenario));
        }
        return results;
    }

    public SimulationResultDto runScenario(FinancialAdvisoryContext baseCtx, SimulationRequest scenario) {
        SimulatedState state = applyScenario(baseCtx, scenario);
        SimulationResultDto result = new SimulationResultDto();
        result.setScenarioName(scenario.getScenarioName() != null ? scenario.getScenarioName() : "What-if");

        BudgetPredictionDto prediction = state.prediction();
        result.setExpectedMonthlySpend(prediction.getExpectedMonthlySpend());
        result.setExpectedSavings(prediction.getExpectedSavings());

        FinancialProfile profile = baseCtx.getHealthContext().getProfile();
        BigDecimal emergency = profile != null ? profile.getEmergencyFundBalance() : BigDecimal.ZERO;
        BigDecimal avgExpense = prediction.getExpectedMonthlySpend().max(BigDecimal.ONE);
        result.setEmergencyFundMonths(
                ScoreMath.safeDiv(emergency, avgExpense).setScale(1, RoundingMode.HALF_UP).doubleValue());

        BigDecimal totalBudget = baseCtx.getCategoryBudgets().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setRemainingBudget(totalBudget.subtract(prediction.getExpectedMonthlySpend()));

        result.setGoalCompletionDate(estimateGoalCompletion(baseCtx.getHealthContext().getGoals()));

        HealthScoreContext adjustedHealth = state.adjustedHealthContext();
        int healthScore = healthScoreAggregator.totalScore(healthScoreAggregator.calculateAll(adjustedHealth));
        result.setHealthScore(healthScore);

        if (scenario.getPurchaseAmount() != null && scenario.getPurchaseAmount().signum() > 0) {
            PurchaseEvaluateDetailRequest req = new PurchaseEvaluateDetailRequest();
            req.setPrice(scenario.getPurchaseAmount());
            req.setCategory(scenario.getPurchaseCategory());
            req.setLabel(scenario.getScenarioName());
            PurchaseDecisionDetailDto decision = purchaseDecisionService.evaluate(state.adjustedContext(), req);
            result.setPurchaseAffordabilityScore(decision.getAffordabilityScore());
            result.setPurchaseDecision(decision.getDecision());
        }

        return result;
    }

    private static LocalDate estimateGoalCompletion(List<SavingsGoal> goals) {
        LocalDate latest = null;
        for (SavingsGoal goal : goals) {
            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
            if (remaining.signum() <= 0) continue;
            long days = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());
            if (days > 0 && (latest == null || goal.getDeadline().isAfter(latest))) {
                latest = goal.getDeadline();
            }
        }
        return latest;
    }

    private SimulatedState applyScenario(FinancialAdvisoryContext base, SimulationRequest scenario) {
        Map<String, BigDecimal> adjustedBudgets = new HashMap<>(base.getCategoryBudgets());
        Map<String, BigDecimal> adjustedSpend = new HashMap<>(base.getHealthContext().getSpentByCategory());

        if (scenario.getCategorySpendAdjustments() != null) {
            for (var e : scenario.getCategorySpendAdjustments().entrySet()) {
                adjustedSpend.merge(e.getKey(), e.getValue(), BigDecimal::add);
            }
        }
        if (scenario.getRentIncrease() != null && scenario.getRentIncrease().signum() != 0) {
            adjustedBudgets.merge("Rent", scenario.getRentIncrease(), BigDecimal::add);
            adjustedSpend.merge("Rent", scenario.getRentIncrease(), BigDecimal::add);
        }

        BigDecimal adjustedIncome = base.getHealthContext().getMonthIncome();
        if (scenario.getSalaryIncrease() != null) {
            adjustedIncome = adjustedIncome.add(scenario.getSalaryIncrease());
        }

        BigDecimal adjustedBalance = base.getCurrentBalance();
        if (scenario.getSalaryIncrease() != null) {
            adjustedBalance = adjustedBalance.add(scenario.getSalaryIncrease());
        }
        if (scenario.getPostponePurchaseDays() != null && scenario.getPostponePurchaseDays() > 0) {
            adjustedBalance = adjustedBalance.add(base.getHealthContext().getMonthIncome());
        } else if (scenario.getPurchaseAmount() != null) {
            adjustedBalance = adjustedBalance.subtract(scenario.getPurchaseAmount());
        }

        return new SimulatedState(base, adjustedBudgets, adjustedSpend, adjustedIncome, adjustedBalance, predictionService);
    }

    /** Mutable overlay for what-if scenarios without cloning entire context graph. */
    private static class SimulatedState {
        private final FinancialAdvisoryContext base;
        private final Map<String, BigDecimal> budgets;
        private final Map<String, BigDecimal> spend;
        private final BigDecimal income;
        private final BigDecimal balance;
        private final PredictionService predictionService;

        SimulatedState(
                FinancialAdvisoryContext base,
                Map<String, BigDecimal> budgets,
                Map<String, BigDecimal> spend,
                BigDecimal income,
                BigDecimal balance,
                PredictionService predictionService) {
            this.base = base;
            this.budgets = budgets;
            this.spend = spend;
            this.income = income;
            this.balance = balance;
            this.predictionService = predictionService;
        }

        FinancialAdvisoryContext adjustedContext() {
            return new FinancialAdvisoryContext(
                    adjustedHealthContext(),
                    base.getAsOfDate(),
                    budgets,
                    base.getHistoricalCategorySpend(),
                    balance,
                    base.getSalaryDayOfMonth(),
                    base.getPlannedPurchases());
        }

        HealthScoreContext adjustedHealthContext() {
            HealthScoreContext orig = base.getHealthContext();
            BigDecimal expenses = spend.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return new HealthScoreContext(
                    orig.getUser(),
                    orig.getMonth(),
                    orig.getMonthTransactions(),
                    orig.getRecentTransactions(),
                    orig.getBudgetItems(),
                    spend,
                    income,
                    expenses,
                    orig.getMonthlyExpenseSeries(),
                    orig.getProfile(),
                    orig.getGoals(),
                    orig.getSubscriptions(),
                    orig.getPreviousScore());
        }

        BudgetPredictionDto prediction() {
            return predictionService.predict(adjustedContext());
        }
    }
}
