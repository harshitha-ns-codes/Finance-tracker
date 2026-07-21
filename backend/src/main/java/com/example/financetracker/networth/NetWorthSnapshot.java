package com.example.financetracker.networth;

import com.example.financetracker.budget.YearMonthStringConverter;
import com.example.financetracker.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(
        name = "net_worth_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_month"}))
public class NetWorthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Convert(converter = YearMonthStringConverter.class)
    @Column(name = "snapshot_month", nullable = false)
    private YearMonth snapshotMonth;

    @Column(name = "total_assets", nullable = false)
    private BigDecimal totalAssets = BigDecimal.ZERO;

    @Column(name = "total_liabilities", nullable = false)
    private BigDecimal totalLiabilities = BigDecimal.ZERO;

    @Column(name = "net_worth", nullable = false)
    private BigDecimal netWorth = BigDecimal.ZERO;

    @Column(name = "liquid_assets", nullable = false)
    private BigDecimal liquidAssets = BigDecimal.ZERO;

    @Column(name = "fixed_assets", nullable = false)
    private BigDecimal fixedAssets = BigDecimal.ZERO;

    @Column(name = "student_loan", nullable = false)
    private BigDecimal studentLoan = BigDecimal.ZERO;

    @Column(name = "credit_card_debt", nullable = false)
    private BigDecimal creditCardDebt = BigDecimal.ZERO;

    @Column(name = "money_owed", nullable = false)
    private BigDecimal moneyOwed = BigDecimal.ZERO;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public YearMonth getSnapshotMonth() {
        return snapshotMonth;
    }

    public void setSnapshotMonth(YearMonth snapshotMonth) {
        this.snapshotMonth = snapshotMonth;
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

    public BigDecimal getMoneyOwed() {
        return moneyOwed;
    }

    public void setMoneyOwed(BigDecimal moneyOwed) {
        this.moneyOwed = moneyOwed;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }
}
