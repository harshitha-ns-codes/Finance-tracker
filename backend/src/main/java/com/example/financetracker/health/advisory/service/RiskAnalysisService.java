package com.example.financetracker.health.advisory.service;

import com.example.financetracker.health.config.HealthScoreProperties;
import org.springframework.stereotype.Service;

@Service
public class RiskAnalysisService {

    private final HealthScoreProperties props;

    public RiskAnalysisService(HealthScoreProperties props) {
        this.props = props;
    }

    /** Risk score = predicted / budget × 100. */
    public double riskScorePercent(double predicted, double budget) {
        if (budget <= 0) {
            return predicted > 0 ? 150 : 0;
        }
        return (predicted / budget) * 100.0;
    }

    public String riskLevel(double riskScorePercent) {
        var risk = props.getRisk();
        if (riskScorePercent < risk.getLowMaxPercent()) return "LOW";
        if (riskScorePercent < risk.getMediumMaxPercent()) return "MEDIUM";
        if (riskScorePercent < risk.getHighMaxPercent()) return "HIGH";
        return "CRITICAL";
    }

    /** Probability of overspending derived deterministically from risk score. */
    public double overspendProbability(double riskScorePercent) {
        if (riskScorePercent <= 70) {
            return Math.max(0, (riskScorePercent - 50) / 20.0 * 30);
        }
        if (riskScorePercent <= 100) {
            return 30 + (riskScorePercent - 70) / 30.0 * 50;
        }
        return Math.min(99, 80 + (riskScorePercent - 100) / 20.0 * 19);
    }
}
