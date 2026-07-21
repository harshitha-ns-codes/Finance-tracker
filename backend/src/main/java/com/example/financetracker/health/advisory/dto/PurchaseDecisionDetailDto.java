package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDecisionDetailDto {
    private int affordabilityScore;
    private String decision;
    private int confidence;
    private String explanation;
    private String reason;
    private LocalDate recommendedPurchaseDate;
    private BigDecimal requiredSavings;
    private int estimatedDaysToAfford;
    private ImpactAnalysisDto impactAnalysis;
    private List<String> alternatives = new ArrayList<>();

    public int getAffordabilityScore() { return affordabilityScore; }
    public void setAffordabilityScore(int affordabilityScore) { this.affordabilityScore = affordabilityScore; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDate getRecommendedPurchaseDate() { return recommendedPurchaseDate; }
    public void setRecommendedPurchaseDate(LocalDate recommendedPurchaseDate) { this.recommendedPurchaseDate = recommendedPurchaseDate; }
    public BigDecimal getRequiredSavings() { return requiredSavings; }
    public void setRequiredSavings(BigDecimal requiredSavings) { this.requiredSavings = requiredSavings; }
    public int getEstimatedDaysToAfford() { return estimatedDaysToAfford; }
    public void setEstimatedDaysToAfford(int estimatedDaysToAfford) { this.estimatedDaysToAfford = estimatedDaysToAfford; }
    public ImpactAnalysisDto getImpactAnalysis() { return impactAnalysis; }
    public void setImpactAnalysis(ImpactAnalysisDto impactAnalysis) { this.impactAnalysis = impactAnalysis; }
    public List<String> getAlternatives() { return alternatives; }
    public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }
}
