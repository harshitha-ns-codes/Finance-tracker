package com.example.financetracker.forecast;

public class SalaryIntelligenceResponse {

    private boolean configured;
    private String zone;
    private Integer daysUntilSalary;
    private Integer salaryDay;
    private Double salaryAmount;
    private Double currentBalance;
    private Double dailyBudget;
    private boolean showAllocation;
    private AllocationPlanDto allocationPlan;
    private int daysInMonth;
    private int todayDayOfMonth;
    private String monthLabel;

    public static SalaryIntelligenceResponse notConfigured() {
        SalaryIntelligenceResponse response = new SalaryIntelligenceResponse();
        response.setConfigured(false);
        return response;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Integer getDaysUntilSalary() {
        return daysUntilSalary;
    }

    public void setDaysUntilSalary(Integer daysUntilSalary) {
        this.daysUntilSalary = daysUntilSalary;
    }

    public Integer getSalaryDay() {
        return salaryDay;
    }

    public void setSalaryDay(Integer salaryDay) {
        this.salaryDay = salaryDay;
    }

    public Double getSalaryAmount() {
        return salaryAmount;
    }

    public void setSalaryAmount(Double salaryAmount) {
        this.salaryAmount = salaryAmount;
    }

    public Double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(Double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public Double getDailyBudget() {
        return dailyBudget;
    }

    public void setDailyBudget(Double dailyBudget) {
        this.dailyBudget = dailyBudget;
    }

    public boolean isShowAllocation() {
        return showAllocation;
    }

    public void setShowAllocation(boolean showAllocation) {
        this.showAllocation = showAllocation;
    }

    public AllocationPlanDto getAllocationPlan() {
        return allocationPlan;
    }

    public void setAllocationPlan(AllocationPlanDto allocationPlan) {
        this.allocationPlan = allocationPlan;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public void setDaysInMonth(int daysInMonth) {
        this.daysInMonth = daysInMonth;
    }

    public int getTodayDayOfMonth() {
        return todayDayOfMonth;
    }

    public void setTodayDayOfMonth(int todayDayOfMonth) {
        this.todayDayOfMonth = todayDayOfMonth;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }
}
