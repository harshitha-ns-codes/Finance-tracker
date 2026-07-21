package com.example.financetracker.goal;

import java.math.BigDecimal;

public class EmergencyFundRecommendationDto {

    private BigDecimal averageMonthlyExpenses;
    private BigDecimal recommendedTarget;
    private String message;

    public BigDecimal getAverageMonthlyExpenses() {
        return averageMonthlyExpenses;
    }

    public void setAverageMonthlyExpenses(BigDecimal averageMonthlyExpenses) {
        this.averageMonthlyExpenses = averageMonthlyExpenses;
    }

    public BigDecimal getRecommendedTarget() {
        return recommendedTarget;
    }

    public void setRecommendedTarget(BigDecimal recommendedTarget) {
        this.recommendedTarget = recommendedTarget;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
