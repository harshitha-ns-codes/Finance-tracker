package com.example.financetracker.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FinancialGoalDto {

    private Long id;
    private String name;
    private GoalType goalType;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private double progressPercent;
    private LocalDate targetDate;
    private String linkedCategory;
    private List<GoalMilestoneDto> milestones = new ArrayList<>();
    private LocalDate projectedCompletionDate;
    private BigDecimal monthlyContributionNeeded;
    private Double monthsAheadBehind;
    private String scheduleMessage;
    private String insight;
    private List<GoalMilestoneDto> newlyAchievedMilestones = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getLinkedCategory() {
        return linkedCategory;
    }

    public void setLinkedCategory(String linkedCategory) {
        this.linkedCategory = linkedCategory;
    }

    public List<GoalMilestoneDto> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<GoalMilestoneDto> milestones) {
        this.milestones = milestones;
    }

    public LocalDate getProjectedCompletionDate() {
        return projectedCompletionDate;
    }

    public void setProjectedCompletionDate(LocalDate projectedCompletionDate) {
        this.projectedCompletionDate = projectedCompletionDate;
    }

    public BigDecimal getMonthlyContributionNeeded() {
        return monthlyContributionNeeded;
    }

    public void setMonthlyContributionNeeded(BigDecimal monthlyContributionNeeded) {
        this.monthlyContributionNeeded = monthlyContributionNeeded;
    }

    public Double getMonthsAheadBehind() {
        return monthsAheadBehind;
    }

    public void setMonthsAheadBehind(Double monthsAheadBehind) {
        this.monthsAheadBehind = monthsAheadBehind;
    }

    public String getScheduleMessage() {
        return scheduleMessage;
    }

    public void setScheduleMessage(String scheduleMessage) {
        this.scheduleMessage = scheduleMessage;
    }

    public String getInsight() {
        return insight;
    }

    public void setInsight(String insight) {
        this.insight = insight;
    }

    public List<GoalMilestoneDto> getNewlyAchievedMilestones() {
        return newlyAchievedMilestones;
    }

    public void setNewlyAchievedMilestones(List<GoalMilestoneDto> newlyAchievedMilestones) {
        this.newlyAchievedMilestones = newlyAchievedMilestones;
    }
}
