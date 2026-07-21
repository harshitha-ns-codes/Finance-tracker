package com.example.financetracker.analyzer;

import java.util.ArrayList;
import java.util.List;

public class Rule502030Response {

    private String month;
    private double totalIncome;
    private BucketAnalysisDto needs;
    private BucketAnalysisDto wants;
    private BucketAnalysisDto savings;
    private double unclassifiedAmount;
    private List<UnclassifiedTransactionDto> unclassifiedTransactions = new ArrayList<>();
    private String overallStatus;
    private String overallInsight;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BucketAnalysisDto getNeeds() {
        return needs;
    }

    public void setNeeds(BucketAnalysisDto needs) {
        this.needs = needs;
    }

    public BucketAnalysisDto getWants() {
        return wants;
    }

    public void setWants(BucketAnalysisDto wants) {
        this.wants = wants;
    }

    public BucketAnalysisDto getSavings() {
        return savings;
    }

    public void setSavings(BucketAnalysisDto savings) {
        this.savings = savings;
    }

    public double getUnclassifiedAmount() {
        return unclassifiedAmount;
    }

    public void setUnclassifiedAmount(double unclassifiedAmount) {
        this.unclassifiedAmount = unclassifiedAmount;
    }

    public List<UnclassifiedTransactionDto> getUnclassifiedTransactions() {
        return unclassifiedTransactions;
    }

    public void setUnclassifiedTransactions(List<UnclassifiedTransactionDto> unclassifiedTransactions) {
        this.unclassifiedTransactions = unclassifiedTransactions;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public String getOverallInsight() {
        return overallInsight;
    }

    public void setOverallInsight(String overallInsight) {
        this.overallInsight = overallInsight;
    }
}
