package com.example.financetracker.transaction;

import com.example.financetracker.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @NotBlank
    private String category;

    private String description;

    @NotNull
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "need_type")
    private NeedType needType = NeedType.UNCLASSIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "regret_status")
    private RegretStatus regretStatus = RegretStatus.NOT_APPLICABLE;

    @Column(name = "regret_review_date")
    private LocalDate regretReviewDate;

    @Column(name = "regret_asked_at")
    private LocalDate regretAskedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public NeedType getNeedType() {
        return needType;
    }

    public void setNeedType(NeedType needType) {
        this.needType = needType;
    }

    public RegretStatus getRegretStatus() {
        return regretStatus;
    }

    public void setRegretStatus(RegretStatus regretStatus) {
        this.regretStatus = regretStatus;
    }

    public LocalDate getRegretReviewDate() {
        return regretReviewDate;
    }

    public void setRegretReviewDate(LocalDate regretReviewDate) {
        this.regretReviewDate = regretReviewDate;
    }

    public LocalDate getRegretAskedAt() {
        return regretAskedAt;
    }

    public void setRegretAskedAt(LocalDate regretAskedAt) {
        this.regretAskedAt = regretAskedAt;
    }
}

