package com.example.financetracker.budget;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetRequest {

    @NotNull
    private String month; // yyyy-MM

    @NotNull
    private BigDecimal monthlyLimit;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}

