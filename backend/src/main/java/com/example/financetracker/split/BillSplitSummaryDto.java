package com.example.financetracker.split;

import java.math.BigDecimal;

public class BillSplitSummaryDto {

    private BigDecimal totalOwedToMe;
    private BigDecimal totalIOwe;
    private BigDecimal netBalance;
    private int pendingCount;
    private int overdueCount;

    public BigDecimal getTotalOwedToMe() {
        return totalOwedToMe;
    }

    public void setTotalOwedToMe(BigDecimal totalOwedToMe) {
        this.totalOwedToMe = totalOwedToMe;
    }

    public BigDecimal getTotalIOwe() {
        return totalIOwe;
    }

    public void setTotalIOwe(BigDecimal totalIOwe) {
        this.totalIOwe = totalIOwe;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }
}
