package com.example.financetracker.goal;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class GoalContributeRequest {

    @NotNull
    private BigDecimal amount;

    private String note;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
