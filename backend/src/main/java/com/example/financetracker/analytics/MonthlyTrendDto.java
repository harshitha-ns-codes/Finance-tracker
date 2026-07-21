package com.example.financetracker.analytics;

import java.math.BigDecimal;

public class MonthlyTrendDto {

    private String month;
    private BigDecimal income;
    private BigDecimal expenses;

    public MonthlyTrendDto() {
    }

    public MonthlyTrendDto(String month, BigDecimal income, BigDecimal expenses) {
        this.month = month;
        this.income = income;
        this.expenses = expenses;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public void setExpenses(BigDecimal expenses) {
        this.expenses = expenses;
    }
}
