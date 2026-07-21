package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BudgetPredictionDto {
    private BigDecimal expectedMonthlySpend;
    private BigDecimal totalBudget;
    private BigDecimal expectedSavings;
    private double overspendProbability;
    private String riskLevel;
    private List<CategoryPredictionDto> categories = new ArrayList<>();

    public BigDecimal getExpectedMonthlySpend() { return expectedMonthlySpend; }
    public void setExpectedMonthlySpend(BigDecimal expectedMonthlySpend) { this.expectedMonthlySpend = expectedMonthlySpend; }
    public BigDecimal getTotalBudget() { return totalBudget; }
    public void setTotalBudget(BigDecimal totalBudget) { this.totalBudget = totalBudget; }
    public BigDecimal getExpectedSavings() { return expectedSavings; }
    public void setExpectedSavings(BigDecimal expectedSavings) { this.expectedSavings = expectedSavings; }
    public double getOverspendProbability() { return overspendProbability; }
    public void setOverspendProbability(double overspendProbability) { this.overspendProbability = overspendProbability; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<CategoryPredictionDto> getCategories() { return categories; }
    public void setCategories(List<CategoryPredictionDto> categories) { this.categories = categories; }
}
