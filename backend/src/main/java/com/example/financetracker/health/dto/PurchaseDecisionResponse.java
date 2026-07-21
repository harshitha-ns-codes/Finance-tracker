package com.example.financetracker.health.dto;

import java.util.ArrayList;
import java.util.List;

public class PurchaseDecisionResponse {
    private String decision; // BUY | WAIT | NOT_RECOMMENDED
    private int confidence;
    private String reason;
    private List<String> alternatives = new ArrayList<>();

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<String> getAlternatives() { return alternatives; }
    public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }
}
