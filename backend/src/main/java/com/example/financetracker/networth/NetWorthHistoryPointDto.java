package com.example.financetracker.networth;

import java.math.BigDecimal;
import java.time.YearMonth;

public class NetWorthHistoryPointDto {

    private String month;
    private BigDecimal netWorth;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;

    public static NetWorthHistoryPointDto from(NetWorthSnapshot snapshot) {
        NetWorthHistoryPointDto dto = new NetWorthHistoryPointDto();
        dto.month = snapshot.getSnapshotMonth().toString();
        dto.netWorth = snapshot.getNetWorth();
        dto.totalAssets = snapshot.getTotalAssets();
        dto.totalLiabilities = snapshot.getTotalLiabilities();
        return dto;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(BigDecimal netWorth) {
        this.netWorth = netWorth;
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
}
