package com.example.financetracker.profile;

public class SalaryProfileResponse {

    private Integer salaryDay;
    private Double salaryAmount;
    private boolean configured;

    public SalaryProfileResponse() {
    }

    public SalaryProfileResponse(Integer salaryDay, Double salaryAmount, boolean configured) {
        this.salaryDay = salaryDay;
        this.salaryAmount = salaryAmount;
        this.configured = configured;
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

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
}
