package com.example.financetracker.transaction;

import java.util.ArrayList;
import java.util.List;

public class RegretStatsResponse {

    private int totalReviewed;
    private int totalRegret;
    private int totalNoRegret;
    private int totalNeutral;
    private double regretRate;
    private String mostRegrettedCategory;
    private String mostValuedCategory;
    private List<CategoryRegretStatsDto> regretByCategory = new ArrayList<>();
    private double totalMoneyRegretted;
    private double averageRegrettedAmount;
    private String recentInsight;

    public int getTotalReviewed() {
        return totalReviewed;
    }

    public void setTotalReviewed(int totalReviewed) {
        this.totalReviewed = totalReviewed;
    }

    public int getTotalRegret() {
        return totalRegret;
    }

    public void setTotalRegret(int totalRegret) {
        this.totalRegret = totalRegret;
    }

    public int getTotalNoRegret() {
        return totalNoRegret;
    }

    public void setTotalNoRegret(int totalNoRegret) {
        this.totalNoRegret = totalNoRegret;
    }

    public int getTotalNeutral() {
        return totalNeutral;
    }

    public void setTotalNeutral(int totalNeutral) {
        this.totalNeutral = totalNeutral;
    }

    public double getRegretRate() {
        return regretRate;
    }

    public void setRegretRate(double regretRate) {
        this.regretRate = regretRate;
    }

    public String getMostRegrettedCategory() {
        return mostRegrettedCategory;
    }

    public void setMostRegrettedCategory(String mostRegrettedCategory) {
        this.mostRegrettedCategory = mostRegrettedCategory;
    }

    public String getMostValuedCategory() {
        return mostValuedCategory;
    }

    public void setMostValuedCategory(String mostValuedCategory) {
        this.mostValuedCategory = mostValuedCategory;
    }

    public List<CategoryRegretStatsDto> getRegretByCategory() {
        return regretByCategory;
    }

    public void setRegretByCategory(List<CategoryRegretStatsDto> regretByCategory) {
        this.regretByCategory = regretByCategory;
    }

    public double getTotalMoneyRegretted() {
        return totalMoneyRegretted;
    }

    public void setTotalMoneyRegretted(double totalMoneyRegretted) {
        this.totalMoneyRegretted = totalMoneyRegretted;
    }

    public double getAverageRegrettedAmount() {
        return averageRegrettedAmount;
    }

    public void setAverageRegrettedAmount(double averageRegrettedAmount) {
        this.averageRegrettedAmount = averageRegrettedAmount;
    }

    public String getRecentInsight() {
        return recentInsight;
    }

    public void setRecentInsight(String recentInsight) {
        this.recentInsight = recentInsight;
    }
}
