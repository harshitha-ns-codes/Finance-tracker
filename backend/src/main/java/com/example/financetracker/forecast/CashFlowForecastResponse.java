package com.example.financetracker.forecast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CashFlowForecastResponse {

    private BigDecimal startingBalance;
    private List<CashFlowDayDto> days = new ArrayList<>();
    private LowestPointDto lowestPoint;
    private boolean willGoNegative;
    private List<String> negativeDates = new ArrayList<>();
    private String summary;

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(BigDecimal startingBalance) {
        this.startingBalance = startingBalance;
    }

    public List<CashFlowDayDto> getDays() {
        return days;
    }

    public void setDays(List<CashFlowDayDto> days) {
        this.days = days;
    }

    public LowestPointDto getLowestPoint() {
        return lowestPoint;
    }

    public void setLowestPoint(LowestPointDto lowestPoint) {
        this.lowestPoint = lowestPoint;
    }

    public boolean isWillGoNegative() {
        return willGoNegative;
    }

    public void setWillGoNegative(boolean willGoNegative) {
        this.willGoNegative = willGoNegative;
    }

    public List<String> getNegativeDates() {
        return negativeDates;
    }

    public void setNegativeDates(List<String> negativeDates) {
        this.negativeDates = negativeDates;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
