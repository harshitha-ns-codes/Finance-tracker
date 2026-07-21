package com.example.financetracker.health.advisory.dto;

import java.util.ArrayList;
import java.util.List;

public class FinancialAdvisoryResponse {
    private BudgetPredictionDto prediction;
    private PurchaseDecisionDetailDto purchaseDecision;
    private List<String> recommendations = new ArrayList<>();
    private List<SimulationResultDto> simulations = new ArrayList<>();

    public BudgetPredictionDto getPrediction() { return prediction; }
    public void setPrediction(BudgetPredictionDto prediction) { this.prediction = prediction; }
    public PurchaseDecisionDetailDto getPurchaseDecision() { return purchaseDecision; }
    public void setPurchaseDecision(PurchaseDecisionDetailDto purchaseDecision) { this.purchaseDecision = purchaseDecision; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public List<SimulationResultDto> getSimulations() { return simulations; }
    public void setSimulations(List<SimulationResultDto> simulations) { this.simulations = simulations; }
}
