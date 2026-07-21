package com.example.financetracker.forecast;

public class AllocationBreakdownItemDto {

    private String label;
    private double amount;
    private String kind;

    public AllocationBreakdownItemDto() {
    }

    public AllocationBreakdownItemDto(String label, double amount, String kind) {
        this.label = label;
        this.amount = amount;
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
