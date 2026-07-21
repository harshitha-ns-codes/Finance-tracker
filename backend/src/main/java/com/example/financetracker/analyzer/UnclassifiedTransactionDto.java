package com.example.financetracker.analyzer;

public class UnclassifiedTransactionDto {

    private Long id;
    private String description;
    private String category;
    private double amount;

    public UnclassifiedTransactionDto() {
    }

    public UnclassifiedTransactionDto(Long id, String description, String category, double amount) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
