package com.example.financetracker.forecast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CashFlowDayDto {

    private String date;
    private BigDecimal projectedBalance;
    private List<String> events = new ArrayList<>();

    public CashFlowDayDto() {
    }

    public CashFlowDayDto(String date, BigDecimal projectedBalance, List<String> events) {
        this.date = date;
        this.projectedBalance = projectedBalance;
        this.events = events != null ? events : new ArrayList<>();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getProjectedBalance() {
        return projectedBalance;
    }

    public void setProjectedBalance(BigDecimal projectedBalance) {
        this.projectedBalance = projectedBalance;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }
}
