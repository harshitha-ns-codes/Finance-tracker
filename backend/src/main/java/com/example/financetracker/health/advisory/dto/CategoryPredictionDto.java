package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;

public class CategoryPredictionDto {
    private String category;
    private BigDecimal budget;
    private BigDecimal currentSpend;
    private BigDecimal predictedSpend;
    private BigDecimal overspendAmount;
    private double riskScorePercent;
    private String riskLevel;
    private BigDecimal dailySpendRate;
    private String recommendation;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public BigDecimal getCurrentSpend() { return currentSpend; }
    public void setCurrentSpend(BigDecimal currentSpend) { this.currentSpend = currentSpend; }
    public BigDecimal getPredictedSpend() { return predictedSpend; }
    public void setPredictedSpend(BigDecimal predictedSpend) { this.predictedSpend = predictedSpend; }
    public BigDecimal getOverspendAmount() { return overspendAmount; }
    public void setOverspendAmount(BigDecimal overspendAmount) { this.overspendAmount = overspendAmount; }
    public double getRiskScorePercent() { return riskScorePercent; }
    public void setRiskScorePercent(double riskScorePercent) { this.riskScorePercent = riskScorePercent; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public BigDecimal getDailySpendRate() { return dailySpendRate; }
    public void setDailySpendRate(BigDecimal dailySpendRate) { this.dailySpendRate = dailySpendRate; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
