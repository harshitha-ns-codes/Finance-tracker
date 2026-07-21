package com.example.financetracker.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PendingRegretDto {

    private Long id;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate date;
    private LocalDate regretReviewDate;

    public PendingRegretDto() {
    }

    public PendingRegretDto(
            Long id,
            String description,
            String category,
            BigDecimal amount,
            LocalDate date,
            LocalDate regretReviewDate) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.regretReviewDate = regretReviewDate;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getRegretReviewDate() {
        return regretReviewDate;
    }

    public void setRegretReviewDate(LocalDate regretReviewDate) {
        this.regretReviewDate = regretReviewDate;
    }
}
