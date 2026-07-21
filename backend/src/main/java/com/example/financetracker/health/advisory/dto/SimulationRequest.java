package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SimulationRequest {
    private String scenarioName;
    private BigDecimal salaryIncrease;
    private BigDecimal rentIncrease;
    private Integer postponePurchaseDays;
    private BigDecimal purchaseAmount;
    private String purchaseCategory;
    private Map<String, BigDecimal> categorySpendAdjustments = new HashMap<>();

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public BigDecimal getSalaryIncrease() { return salaryIncrease; }
    public void setSalaryIncrease(BigDecimal salaryIncrease) { this.salaryIncrease = salaryIncrease; }
    public BigDecimal getRentIncrease() { return rentIncrease; }
    public void setRentIncrease(BigDecimal rentIncrease) { this.rentIncrease = rentIncrease; }
    public Integer getPostponePurchaseDays() { return postponePurchaseDays; }
    public void setPostponePurchaseDays(Integer postponePurchaseDays) { this.postponePurchaseDays = postponePurchaseDays; }
    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public void setPurchaseAmount(BigDecimal purchaseAmount) { this.purchaseAmount = purchaseAmount; }
    public String getPurchaseCategory() { return purchaseCategory; }
    public void setPurchaseCategory(String purchaseCategory) { this.purchaseCategory = purchaseCategory; }
    public Map<String, BigDecimal> getCategorySpendAdjustments() { return categorySpendAdjustments; }
    public void setCategorySpendAdjustments(Map<String, BigDecimal> categorySpendAdjustments) { this.categorySpendAdjustments = categorySpendAdjustments; }
}
