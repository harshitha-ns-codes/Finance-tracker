package com.example.financetracker.transaction;

public class CategoryRegretStatsDto {

    private String category;
    private int reviewed;
    private int regret;
    private double regretRate;

    public CategoryRegretStatsDto() {
    }

    public CategoryRegretStatsDto(String category, int reviewed, int regret, double regretRate) {
        this.category = category;
        this.reviewed = reviewed;
        this.regret = regret;
        this.regretRate = regretRate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getReviewed() {
        return reviewed;
    }

    public void setReviewed(int reviewed) {
        this.reviewed = reviewed;
    }

    public int getRegret() {
        return regret;
    }

    public void setRegret(int regret) {
        this.regret = regret;
    }

    public double getRegretRate() {
        return regretRate;
    }

    public void setRegretRate(double regretRate) {
        this.regretRate = regretRate;
    }
}
