package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImpactAnalysisDto {
    private BigDecimal balanceAfterPurchase;
    private BigDecimal remainingBudgetAfter;
    private double emergencyFundMonthsAfter;
    private int savingsGoalDelayDays;
    private boolean upcomingBillsCovered;
    private BigDecimal expectedEndOfMonthBalance;
    private double budgetUtilizationBeforePercent;
    private double budgetUtilizationAfterPercent;
    private Map<String, Integer> componentScores = new LinkedHashMap<>();

    public BigDecimal getBalanceAfterPurchase() { return balanceAfterPurchase; }
    public void setBalanceAfterPurchase(BigDecimal balanceAfterPurchase) { this.balanceAfterPurchase = balanceAfterPurchase; }
    public BigDecimal getRemainingBudgetAfter() { return remainingBudgetAfter; }
    public void setRemainingBudgetAfter(BigDecimal remainingBudgetAfter) { this.remainingBudgetAfter = remainingBudgetAfter; }
    public double getEmergencyFundMonthsAfter() { return emergencyFundMonthsAfter; }
    public void setEmergencyFundMonthsAfter(double emergencyFundMonthsAfter) { this.emergencyFundMonthsAfter = emergencyFundMonthsAfter; }
    public int getSavingsGoalDelayDays() { return savingsGoalDelayDays; }
    public void setSavingsGoalDelayDays(int savingsGoalDelayDays) { this.savingsGoalDelayDays = savingsGoalDelayDays; }
    public boolean isUpcomingBillsCovered() { return upcomingBillsCovered; }
    public void setUpcomingBillsCovered(boolean upcomingBillsCovered) { this.upcomingBillsCovered = upcomingBillsCovered; }
    public BigDecimal getExpectedEndOfMonthBalance() { return expectedEndOfMonthBalance; }
    public void setExpectedEndOfMonthBalance(BigDecimal expectedEndOfMonthBalance) { this.expectedEndOfMonthBalance = expectedEndOfMonthBalance; }
    public double getBudgetUtilizationBeforePercent() { return budgetUtilizationBeforePercent; }
    public void setBudgetUtilizationBeforePercent(double budgetUtilizationBeforePercent) { this.budgetUtilizationBeforePercent = budgetUtilizationBeforePercent; }
    public double getBudgetUtilizationAfterPercent() { return budgetUtilizationAfterPercent; }
    public void setBudgetUtilizationAfterPercent(double budgetUtilizationAfterPercent) { this.budgetUtilizationAfterPercent = budgetUtilizationAfterPercent; }
    public Map<String, Integer> getComponentScores() { return componentScores; }
    public void setComponentScores(Map<String, Integer> componentScores) { this.componentScores = componentScores; }
}
