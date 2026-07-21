package com.example.financetracker.analyzer;

public class CategoryAmountDto {

    private String category;
    private double amount;

    public CategoryAmountDto() {
    }

    public CategoryAmountDto(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
