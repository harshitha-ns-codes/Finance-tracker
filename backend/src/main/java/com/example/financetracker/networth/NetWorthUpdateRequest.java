package com.example.financetracker.networth;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NetWorthUpdateRequest {

    private boolean useAutoBankBalance = true;
    private BigDecimal bankBalance;
    private BigDecimal fixedDeposits;
    private BigDecimal investments;
    private List<NamedAmountDto> physicalAssets = new ArrayList<>();
    private BigDecimal studentLoan;
    private BigDecimal creditCardDebt;
    private List<NamedAmountDto> moneyOwed = new ArrayList<>();

    public boolean isUseAutoBankBalance() {
        return useAutoBankBalance;
    }

    public void setUseAutoBankBalance(boolean useAutoBankBalance) {
        this.useAutoBankBalance = useAutoBankBalance;
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
}
