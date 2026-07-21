package com.example.financetracker.networth;

import java.math.BigDecimal;

public class BreakdownItemDto {

    private String label;
    private BigDecimal amount;
    private double percent;

    public BreakdownItemDto() {
    }

    public BreakdownItemDto(String label, BigDecimal amount, double percent) {
        this.label = label;
        this.amount = amount;
        this.percent = percent;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }
}
