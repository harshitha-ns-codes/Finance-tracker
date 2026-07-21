package com.example.financetracker.simulate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PurchaseSimulationResponse {

    private boolean canAfford;
    private int affordabilityScore;

    private ImmediateImpact immediateImpact;
    private BudgetImpact budgetImpact;
    private HealthScoreImpact healthScoreImpact;
    private SavingsImpact savingsImpact;

    private String verdict; // GO_AHEAD | CONSIDER | AVOID
    private String verdictReason;
    private List<String> alternatives = new ArrayList<>();

    public boolean isCanAfford() {
        return canAfford;
    }

    public void setCanAfford(boolean canAfford) {
        this.canAfford = canAfford;
    }

    public int getAffordabilityScore() {
        return affordabilityScore;
    }

    public void setAffordabilityScore(int affordabilityScore) {
        this.affordabilityScore = affordabilityScore;
    }

    public ImmediateImpact getImmediateImpact() {
        return immediateImpact;
    }

    public void setImmediateImpact(ImmediateImpact immediateImpact) {
        this.immediateImpact = immediateImpact;
    }

    public BudgetImpact getBudgetImpact() {
        return budgetImpact;
    }

    public void setBudgetImpact(BudgetImpact budgetImpact) {
        this.budgetImpact = budgetImpact;
    }

    public HealthScoreImpact getHealthScoreImpact() {
        return healthScoreImpact;
    }

    public void setHealthScoreImpact(HealthScoreImpact healthScoreImpact) {
        this.healthScoreImpact = healthScoreImpact;
    }

    public SavingsImpact getSavingsImpact() {
        return savingsImpact;
    }

    public void setSavingsImpact(SavingsImpact savingsImpact) {
        this.savingsImpact = savingsImpact;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getVerdictReason() {
        return verdictReason;
    }

    public void setVerdictReason(String verdictReason) {
        this.verdictReason = verdictReason;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<String> alternatives) {
        this.alternatives = alternatives;
    }

    public static class ImmediateImpact {
        private BigDecimal newBalance;
        private BigDecimal balanceChange;
        private double percentOfBalance;

        public BigDecimal getNewBalance() {
            return newBalance;
        }

        public void setNewBalance(BigDecimal newBalance) {
            this.newBalance = newBalance;
        }

        public BigDecimal getBalanceChange() {
            return balanceChange;
        }

        public void setBalanceChange(BigDecimal balanceChange) {
            this.balanceChange = balanceChange;
        }

        public double getPercentOfBalance() {
            return percentOfBalance;
        }

        public void setPercentOfBalance(double percentOfBalance) {
            this.percentOfBalance = percentOfBalance;
        }
    }

    public static class BudgetImpact {
        private BigDecimal categoryBudget;
        private BigDecimal categorySpent;
        private BigDecimal afterPurchase;
        private boolean willExceedBudget;
        private BigDecimal exceedBy;

        public BigDecimal getCategoryBudget() {
            return categoryBudget;
        }

        public void setCategoryBudget(BigDecimal categoryBudget) {
            this.categoryBudget = categoryBudget;
        }

        public BigDecimal getCategorySpent() {
            return categorySpent;
        }

        public void setCategorySpent(BigDecimal categorySpent) {
            this.categorySpent = categorySpent;
        }

        public BigDecimal getAfterPurchase() {
            return afterPurchase;
        }

        public void setAfterPurchase(BigDecimal afterPurchase) {
            this.afterPurchase = afterPurchase;
        }

        public boolean isWillExceedBudget() {
            return willExceedBudget;
        }

        public void setWillExceedBudget(boolean willExceedBudget) {
            this.willExceedBudget = willExceedBudget;
        }

        public BigDecimal getExceedBy() {
            return exceedBy;
        }

        public void setExceedBy(BigDecimal exceedBy) {
            this.exceedBy = exceedBy;
        }
    }

    public static class HealthScoreImpact {
        private int currentScore;
        private int projectedScore;
        private int change;
        private String reason;

        public int getCurrentScore() {
            return currentScore;
        }

        public void setCurrentScore(int currentScore) {
            this.currentScore = currentScore;
        }

        public int getProjectedScore() {
            return projectedScore;
        }

        public void setProjectedScore(int projectedScore) {
            this.projectedScore = projectedScore;
        }

        public int getChange() {
            return change;
        }

        public void setChange(int change) {
            this.change = change;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class SavingsImpact {
        private BigDecimal currentMonthlySavings;
        private BigDecimal afterPurchaseSavings;
        private double currentSavingsRate;
        private double afterSavingsRate;
        private double savingsRateChange;

        public BigDecimal getCurrentMonthlySavings() {
            return currentMonthlySavings;
        }

        public void setCurrentMonthlySavings(BigDecimal currentMonthlySavings) {
            this.currentMonthlySavings = currentMonthlySavings;
        }

        public BigDecimal getAfterPurchaseSavings() {
            return afterPurchaseSavings;
        }

        public void setAfterPurchaseSavings(BigDecimal afterPurchaseSavings) {
            this.afterPurchaseSavings = afterPurchaseSavings;
        }

        public double getCurrentSavingsRate() {
            return currentSavingsRate;
        }

        public void setCurrentSavingsRate(double currentSavingsRate) {
            this.currentSavingsRate = currentSavingsRate;
        }

        public double getAfterSavingsRate() {
            return afterSavingsRate;
        }

        public void setAfterSavingsRate(double afterSavingsRate) {
            this.afterSavingsRate = afterSavingsRate;
        }

        public double getSavingsRateChange() {
            return savingsRateChange;
        }

        public void setSavingsRateChange(double savingsRateChange) {
            this.savingsRateChange = savingsRateChange;
        }
    }
}
