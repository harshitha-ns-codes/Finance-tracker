package com.example.financetracker.analyzer;

import java.util.ArrayList;
import java.util.List;

public class BucketAnalysisDto {

    private double amount;
    private double percent;
    private double idealPercent;
    private double diff;
    private String status;
    private String insight;
    private List<CategoryAmountDto> topCategories = new ArrayList<>();

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public double getIdealPercent() {
        return idealPercent;
    }

    public void setIdealPercent(double idealPercent) {
        this.idealPercent = idealPercent;
    }

    public double getDiff() {
        return diff;
    }

    public void setDiff(double diff) {
        this.diff = diff;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInsight() {
        return insight;
    }

    public void setInsight(String insight) {
        this.insight = insight;
    }

    public List<CategoryAmountDto> getTopCategories() {
        return topCategories;
    }

    public void setTopCategories(List<CategoryAmountDto> topCategories) {
        this.topCategories = topCategories;
    }
}
