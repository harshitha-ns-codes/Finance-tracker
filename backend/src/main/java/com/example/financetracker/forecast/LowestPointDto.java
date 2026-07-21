package com.example.financetracker.forecast;

import java.math.BigDecimal;

public class LowestPointDto {

    private String date;
    private BigDecimal balance;

    public LowestPointDto() {
    }

    public LowestPointDto(String date, BigDecimal balance) {
        this.date = date;
        this.balance = balance;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
