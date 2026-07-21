package com.example.financetracker.forecast;

import java.util.ArrayList;
import java.util.List;

public class AllocationPlanDto {

    private double salaryAmount;
    private double fixedCosts;
    private double suggestedSavings;
    private double discretionary;
    private List<AllocationBreakdownItemDto> breakdown = new ArrayList<>();

    public double getSalaryAmount() {
        return salaryAmount;
    }

    public void setSalaryAmount(double salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    public double getFixedCosts() {
        return fixedCosts;
    }

    public void setFixedCosts(double fixedCosts) {
        this.fixedCosts = fixedCosts;
    }

    public double getSuggestedSavings() {
        return suggestedSavings;
    }

    public void setSuggestedSavings(double suggestedSavings) {
        this.suggestedSavings = suggestedSavings;
    }

    public double getDiscretionary() {
        return discretionary;
    }

    public void setDiscretionary(double discretionary) {
        this.discretionary = discretionary;
    }

    public List<AllocationBreakdownItemDto> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<AllocationBreakdownItemDto> breakdown) {
        this.breakdown = breakdown;
    }
}
