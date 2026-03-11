package com.example.financetracker.analytics;

import java.math.BigDecimal;

public class DashboardSummary {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private String topSpendingCategory;
    private BigDecimal topSpendingAmount;
    private BigDecimal monthlyBudgetLimit;
    private BigDecimal monthlyExpenses;
    private boolean nearBudgetLimit;

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getTopSpendingCategory() {
        return topSpendingCategory;
    }

    public void setTopSpendingCategory(String topSpendingCategory) {
        this.topSpendingCategory = topSpendingCategory;
    }

    public BigDecimal getTopSpendingAmount() {
        return topSpendingAmount;
    }

    public void setTopSpendingAmount(BigDecimal topSpendingAmount) {
        this.topSpendingAmount = topSpendingAmount;
    }

    public BigDecimal getMonthlyBudgetLimit() {
        return monthlyBudgetLimit;
    }

    public void setMonthlyBudgetLimit(BigDecimal monthlyBudgetLimit) {
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public boolean isNearBudgetLimit() {
        return nearBudgetLimit;
    }

    public void setNearBudgetLimit(boolean nearBudgetLimit) {
        this.nearBudgetLimit = nearBudgetLimit;
    }
}

