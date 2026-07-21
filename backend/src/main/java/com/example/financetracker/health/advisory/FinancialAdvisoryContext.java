package com.example.financetracker.health.advisory;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.model.PlannedPurchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Extended snapshot for prediction, simulation, and purchase decisions. */
public class FinancialAdvisoryContext {

    private final HealthScoreContext healthContext;
    private final LocalDate asOfDate;
    private final Map<String, BigDecimal> categoryBudgets;
    private final Map<String, List<BigDecimal>> historicalCategorySpend;
    private final BigDecimal currentBalance;
    private final int salaryDayOfMonth;
    private final List<PlannedPurchase> plannedPurchases;

    public FinancialAdvisoryContext(
            HealthScoreContext healthContext,
            LocalDate asOfDate,
            Map<String, BigDecimal> categoryBudgets,
            Map<String, List<BigDecimal>> historicalCategorySpend,
            BigDecimal currentBalance,
            int salaryDayOfMonth,
            List<PlannedPurchase> plannedPurchases) {
        this.healthContext = healthContext;
        this.asOfDate = asOfDate;
        this.categoryBudgets = categoryBudgets;
        this.historicalCategorySpend = historicalCategorySpend;
        this.currentBalance = currentBalance;
        this.salaryDayOfMonth = salaryDayOfMonth;
        this.plannedPurchases = plannedPurchases;
    }

    public HealthScoreContext getHealthContext() { return healthContext; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public Map<String, BigDecimal> getCategoryBudgets() { return categoryBudgets; }
    public Map<String, List<BigDecimal>> getHistoricalCategorySpend() { return historicalCategorySpend; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public int getSalaryDayOfMonth() { return salaryDayOfMonth; }
    public List<PlannedPurchase> getPlannedPurchases() { return plannedPurchases; }
    public List<CategoryBudgetItem> getBudgetItems() { return healthContext.getBudgetItems(); }
}
