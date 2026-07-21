package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SimulationResultDto {
    private String scenarioName;
    private Integer healthScore;
    private BigDecimal expectedMonthlySpend;
    private BigDecimal expectedSavings;
    private double emergencyFundMonths;
    private BigDecimal remainingBudget;
    private LocalDate goalCompletionDate;
    private Integer purchaseAffordabilityScore;
    private String purchaseDecision;

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    public BigDecimal getExpectedMonthlySpend() { return expectedMonthlySpend; }
    public void setExpectedMonthlySpend(BigDecimal expectedMonthlySpend) { this.expectedMonthlySpend = expectedMonthlySpend; }
    public BigDecimal getExpectedSavings() { return expectedSavings; }
    public void setExpectedSavings(BigDecimal expectedSavings) { this.expectedSavings = expectedSavings; }
    public double getEmergencyFundMonths() { return emergencyFundMonths; }
    public void setEmergencyFundMonths(double emergencyFundMonths) { this.emergencyFundMonths = emergencyFundMonths; }
    public BigDecimal getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(BigDecimal remainingBudget) { this.remainingBudget = remainingBudget; }
    public LocalDate getGoalCompletionDate() { return goalCompletionDate; }
    public void setGoalCompletionDate(LocalDate goalCompletionDate) { this.goalCompletionDate = goalCompletionDate; }
    public Integer getPurchaseAffordabilityScore() { return purchaseAffordabilityScore; }
    public void setPurchaseAffordabilityScore(Integer purchaseAffordabilityScore) { this.purchaseAffordabilityScore = purchaseAffordabilityScore; }
    public String getPurchaseDecision() { return purchaseDecision; }
    public void setPurchaseDecision(String purchaseDecision) { this.purchaseDecision = purchaseDecision; }
}
