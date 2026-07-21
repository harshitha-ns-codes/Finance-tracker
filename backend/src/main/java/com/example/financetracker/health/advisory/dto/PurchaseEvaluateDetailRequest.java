package com.example.financetracker.health.advisory.dto;

import java.math.BigDecimal;

public class PurchaseEvaluateDetailRequest {
    private BigDecimal price;
    private String label;
    private String category;
    /** NECESSITY | WANT | LUXURY */
    private String priority;

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
