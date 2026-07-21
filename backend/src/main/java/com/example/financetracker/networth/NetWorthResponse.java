package com.example.financetracker.networth;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NetWorthResponse {

    private boolean useAutoBankBalance;
    private BigDecimal autoBankBalance;
    private BigDecimal bankBalance;
    private BigDecimal fixedDeposits;
    private BigDecimal investments;
    private List<NamedAmountDto> physicalAssets = new ArrayList<>();
    private BigDecimal studentLoan;
    private BigDecimal creditCardDebt;
    private List<NamedAmountDto> moneyOwed = new ArrayList<>();

    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private BigDecimal liquidAssets;
    private BigDecimal fixedAssets;
    private double liquidPercent;
    private double fixedPercent;

    private List<BreakdownItemDto> assetBreakdown = new ArrayList<>();
    private List<BreakdownItemDto> liabilityBreakdown = new ArrayList<>();

    private BigDecimal monthOverMonthChange;
    private String monthOverMonthMessage;

    private List<NetWorthHistoryPointDto> history = new ArrayList<>();

    public boolean isUseAutoBankBalance() {
        return useAutoBankBalance;
    }

    public void setUseAutoBankBalance(boolean useAutoBankBalance) {
        this.useAutoBankBalance = useAutoBankBalance;
    }

    public BigDecimal getAutoBankBalance() {
        return autoBankBalance;
    }

    public void setAutoBankBalance(BigDecimal autoBankBalance) {
        this.autoBankBalance = autoBankBalance;
    }

    public BigDecimal getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(BigDecimal bankBalance) {
        this.bankBalance = bankBalance;
    }

    public BigDecimal getFixedDeposits() {
        return fixedDeposits;
    }

    public void setFixedDeposits(BigDecimal fixedDeposits) {
        this.fixedDeposits = fixedDeposits;
    }

    public BigDecimal getInvestments() {
        return investments;
    }

    public void setInvestments(BigDecimal investments) {
        this.investments = investments;
    }

    public List<NamedAmountDto> getPhysicalAssets() {
        return physicalAssets;
    }

    public void setPhysicalAssets(List<NamedAmountDto> physicalAssets) {
        this.physicalAssets = physicalAssets;
    }

    public BigDecimal getStudentLoan() {
        return studentLoan;
    }

    public void setStudentLoan(BigDecimal studentLoan) {
        this.studentLoan = studentLoan;
    }

    public BigDecimal getCreditCardDebt() {
        return creditCardDebt;
    }

    public void setCreditCardDebt(BigDecimal creditCardDebt) {
        this.creditCardDebt = creditCardDebt;
    }

    public List<NamedAmountDto> getMoneyOwed() {
        return moneyOwed;
    }

    public void setMoneyOwed(List<NamedAmountDto> moneyOwed) {
        this.moneyOwed = moneyOwed;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public BigDecimal getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(BigDecimal netWorth) {
        this.netWorth = netWorth;
    }

    public BigDecimal getLiquidAssets() {
        return liquidAssets;
    }

    public void setLiquidAssets(BigDecimal liquidAssets) {
        this.liquidAssets = liquidAssets;
    }

    public BigDecimal getFixedAssets() {
        return fixedAssets;
    }

    public void setFixedAssets(BigDecimal fixedAssets) {
        this.fixedAssets = fixedAssets;
    }

    public double getLiquidPercent() {
        return liquidPercent;
    }

    public void setLiquidPercent(double liquidPercent) {
        this.liquidPercent = liquidPercent;
    }

    public double getFixedPercent() {
        return fixedPercent;
    }

    public void setFixedPercent(double fixedPercent) {
        this.fixedPercent = fixedPercent;
    }

    public List<BreakdownItemDto> getAssetBreakdown() {
        return assetBreakdown;
    }

    public void setAssetBreakdown(List<BreakdownItemDto> assetBreakdown) {
        this.assetBreakdown = assetBreakdown;
    }

    public List<BreakdownItemDto> getLiabilityBreakdown() {
        return liabilityBreakdown;
    }

    public void setLiabilityBreakdown(List<BreakdownItemDto> liabilityBreakdown) {
        this.liabilityBreakdown = liabilityBreakdown;
    }

    public BigDecimal getMonthOverMonthChange() {
        return monthOverMonthChange;
    }

    public void setMonthOverMonthChange(BigDecimal monthOverMonthChange) {
        this.monthOverMonthChange = monthOverMonthChange;
    }

    public String getMonthOverMonthMessage() {
        return monthOverMonthMessage;
    }

    public void setMonthOverMonthMessage(String monthOverMonthMessage) {
        this.monthOverMonthMessage = monthOverMonthMessage;
    }

    public List<NetWorthHistoryPointDto> getHistory() {
        return history;
    }

    public void setHistory(List<NetWorthHistoryPointDto> history) {
        this.history = history;
    }
}
