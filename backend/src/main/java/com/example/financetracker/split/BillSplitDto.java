package com.example.financetracker.split;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class BillSplitDto {

    private UUID id;
    private String title;
    private BigDecimal totalAmount;
    private BigDecimal myShare;
    private String otherPersonName;
    private BigDecimal otherPersonAmount;
    private SplitType splitType;
    private SplitStatus status;
    private String category;
    private LocalDate date;
    private LocalDate settledDate;
    private String notes;
    private int daysAgo;
    private boolean overdue;

    public static BillSplitDto from(BillSplit split) {
        BillSplitDto dto = new BillSplitDto();
        dto.id = split.getId();
        dto.title = split.getTitle();
        dto.totalAmount = split.getTotalAmount();
        dto.myShare = split.getMyShare();
        dto.otherPersonName = split.getOtherPersonName();
        dto.otherPersonAmount = split.getOtherPersonAmount();
        dto.splitType = split.getSplitType();
        dto.status = split.getStatus();
        dto.category = split.getCategory();
        dto.date = split.getDate();
        dto.settledDate = split.getSettledDate();
        dto.notes = split.getNotes();
        dto.daysAgo = BillSplitService.daysSince(split.getDate());
        dto.overdue = BillSplitService.isOverdue(split);
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getMyShare() {
        return myShare;
    }

    public void setMyShare(BigDecimal myShare) {
        this.myShare = myShare;
    }

    public String getOtherPersonName() {
        return otherPersonName;
    }

    public void setOtherPersonName(String otherPersonName) {
        this.otherPersonName = otherPersonName;
    }

    public BigDecimal getOtherPersonAmount() {
        return otherPersonAmount;
    }

    public void setOtherPersonAmount(BigDecimal otherPersonAmount) {
        this.otherPersonAmount = otherPersonAmount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public SplitStatus getStatus() {
        return status;
    }

    public void setStatus(SplitStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(LocalDate settledDate) {
        this.settledDate = settledDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(int daysAgo) {
        this.daysAgo = daysAgo;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}
