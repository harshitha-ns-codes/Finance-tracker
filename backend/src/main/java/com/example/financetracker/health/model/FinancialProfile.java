package com.example.financetracker.health.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "financial_profiles")
public class FinancialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal emergencyFundBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal monthlyDebtPayments = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalDebtOutstanding = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    /** Day of month salary is credited (1–31). */
    @Column(name = "salary_day")
    private Integer salaryDayOfMonth = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getEmergencyFundBalance() { return emergencyFundBalance; }
    public void setEmergencyFundBalance(BigDecimal emergencyFundBalance) { this.emergencyFundBalance = emergencyFundBalance; }
    public BigDecimal getMonthlyDebtPayments() { return monthlyDebtPayments; }
    public void setMonthlyDebtPayments(BigDecimal monthlyDebtPayments) { this.monthlyDebtPayments = monthlyDebtPayments; }
    public BigDecimal getTotalDebtOutstanding() { return totalDebtOutstanding; }
    public void setTotalDebtOutstanding(BigDecimal totalDebtOutstanding) { this.totalDebtOutstanding = totalDebtOutstanding; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public Integer getSalaryDayOfMonth() { return salaryDayOfMonth; }
    public void setSalaryDayOfMonth(Integer salaryDayOfMonth) { this.salaryDayOfMonth = salaryDayOfMonth; }
}
