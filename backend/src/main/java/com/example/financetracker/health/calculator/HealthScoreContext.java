package com.example.financetracker.health.calculator;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.model.Subscription;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.user.User;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Immutable snapshot of inputs used by every score component. */
public class HealthScoreContext {

    private final User user;
    private final YearMonth month;
    private final List<Transaction> monthTransactions;
    private final List<Transaction> recentTransactions;
    private final List<CategoryBudgetItem> budgetItems;
    private final Map<String, BigDecimal> spentByCategory;
    private final BigDecimal monthIncome;
    private final BigDecimal monthExpenses;
    private final List<BigDecimal> monthlyExpenseSeries;
    private final FinancialProfile profile;
    private final List<SavingsGoal> goals;
    private final List<Subscription> subscriptions;
    private final Integer previousScore;

    public HealthScoreContext(
            User user,
            YearMonth month,
            List<Transaction> monthTransactions,
            List<Transaction> recentTransactions,
            List<CategoryBudgetItem> budgetItems,
            Map<String, BigDecimal> spentByCategory,
            BigDecimal monthIncome,
            BigDecimal monthExpenses,
            List<BigDecimal> monthlyExpenseSeries,
            FinancialProfile profile,
            List<SavingsGoal> goals,
            List<Subscription> subscriptions,
            Integer previousScore) {
        this.user = user;
        this.month = month;
        this.monthTransactions = monthTransactions;
        this.recentTransactions = recentTransactions;
        this.budgetItems = budgetItems;
        this.spentByCategory = spentByCategory;
        this.monthIncome = monthIncome;
        this.monthExpenses = monthExpenses;
        this.monthlyExpenseSeries = monthlyExpenseSeries;
        this.profile = profile;
        this.goals = goals;
        this.subscriptions = subscriptions;
        this.previousScore = previousScore;
    }

    public User getUser() { return user; }
    public YearMonth getMonth() { return month; }
    public List<Transaction> getMonthTransactions() { return monthTransactions; }
    public List<Transaction> getRecentTransactions() { return recentTransactions; }
    public List<CategoryBudgetItem> getBudgetItems() { return budgetItems; }
    public Map<String, BigDecimal> getSpentByCategory() { return spentByCategory; }
    public BigDecimal getMonthIncome() { return monthIncome; }
    public BigDecimal getMonthExpenses() { return monthExpenses; }
    public List<BigDecimal> getMonthlyExpenseSeries() { return monthlyExpenseSeries; }
    public FinancialProfile getProfile() { return profile; }
    public List<SavingsGoal> getGoals() { return goals; }
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public Integer getPreviousScore() { return previousScore; }

    public BigDecimal monthSavings() {
        return monthIncome.subtract(monthExpenses);
    }
}
