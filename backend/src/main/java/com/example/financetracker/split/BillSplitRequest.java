package com.example.financetracker.split;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillSplitRequest {

    @NotBlank
    private String title;

    @NotNull
    private BigDecimal totalAmount;

    @NotNull
    private BigDecimal myShare;

    @NotBlank
    private String otherPersonName;

    @NotNull
    private BigDecimal otherPersonAmount;

    @NotNull
    private SplitType splitType;

    @NotBlank
    private String category;

    @NotNull
    private LocalDate date;

    private String notes;

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
