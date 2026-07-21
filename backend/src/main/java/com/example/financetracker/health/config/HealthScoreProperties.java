package com.example.financetracker.health.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finance.health")
public class HealthScoreProperties {

    private Weights weights = new Weights();
    private Ratings ratings = new Ratings();
    private SavingsRate savingsRate = new SavingsRate();
    private EmergencyFund emergencyFund = new EmergencyFund();
    private SpendingConsistency spendingConsistency = new SpendingConsistency();
    private Debt debt = new Debt();
    private Subscriptions subscriptions = new Subscriptions();
    private Decision decision = new Decision();
    private Prediction prediction = new Prediction();
    private Risk risk = new Risk();
    private Affordability affordability = new Affordability();

    public Weights getWeights() { return weights; }
    public void setWeights(Weights weights) { this.weights = weights; }
    public Ratings getRatings() { return ratings; }
    public void setRatings(Ratings ratings) { this.ratings = ratings; }
    public SavingsRate getSavingsRate() { return savingsRate; }
    public void setSavingsRate(SavingsRate savingsRate) { this.savingsRate = savingsRate; }
    public EmergencyFund getEmergencyFund() { return emergencyFund; }
    public void setEmergencyFund(EmergencyFund emergencyFund) { this.emergencyFund = emergencyFund; }
    public SpendingConsistency getSpendingConsistency() { return spendingConsistency; }
    public void setSpendingConsistency(SpendingConsistency spendingConsistency) { this.spendingConsistency = spendingConsistency; }
    public Debt getDebt() { return debt; }
    public void setDebt(Debt debt) { this.debt = debt; }
    public Subscriptions getSubscriptions() { return subscriptions; }
    public void setSubscriptions(Subscriptions subscriptions) { this.subscriptions = subscriptions; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public Prediction getPrediction() { return prediction; }
    public void setPrediction(Prediction prediction) { this.prediction = prediction; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
    public Affordability getAffordability() { return affordability; }
    public void setAffordability(Affordability affordability) { this.affordability = affordability; }

    public static class Weights {
        private int budgetAdherence = 30;
        private int savingsRate = 20;
        private int emergencyFund = 10;
        private int billDiscipline = 10;
        private int spendingConsistency = 10;
        private int goalProgress = 10;
        private int debtHealth = 5;
        private int subscriptionHealth = 5;

        public int getBudgetAdherence() { return budgetAdherence; }
        public void setBudgetAdherence(int budgetAdherence) { this.budgetAdherence = budgetAdherence; }
        public int getSavingsRate() { return savingsRate; }
        public void setSavingsRate(int savingsRate) { this.savingsRate = savingsRate; }
        public int getEmergencyFund() { return emergencyFund; }
        public void setEmergencyFund(int emergencyFund) { this.emergencyFund = emergencyFund; }
        public int getBillDiscipline() { return billDiscipline; }
        public void setBillDiscipline(int billDiscipline) { this.billDiscipline = billDiscipline; }
        public int getSpendingConsistency() { return spendingConsistency; }
        public void setSpendingConsistency(int spendingConsistency) { this.spendingConsistency = spendingConsistency; }
        public int getGoalProgress() { return goalProgress; }
        public void setGoalProgress(int goalProgress) { this.goalProgress = goalProgress; }
        public int getDebtHealth() { return debtHealth; }
        public void setDebtHealth(int debtHealth) { this.debtHealth = debtHealth; }
        public int getSubscriptionHealth() { return subscriptionHealth; }
        public void setSubscriptionHealth(int subscriptionHealth) { this.subscriptionHealth = subscriptionHealth; }

        public int total() {
            return budgetAdherence + savingsRate + emergencyFund + billDiscipline
                    + spendingConsistency + goalProgress + debtHealth + subscriptionHealth;
        }
    }

    public static class Ratings {
        private int excellentMin = 80;
        private int goodMin = 65;
        private int fairMin = 50;
        public int getExcellentMin() { return excellentMin; }
        public void setExcellentMin(int excellentMin) { this.excellentMin = excellentMin; }
        public int getGoodMin() { return goodMin; }
        public void setGoodMin(int goodMin) { this.goodMin = goodMin; }
        public int getFairMin() { return fairMin; }
        public void setFairMin(int fairMin) { this.fairMin = fairMin; }
    }

    public static class SavingsRate {
        private double fullScorePercent = 30;
        private double zeroScorePercent = 0;
        public double getFullScorePercent() { return fullScorePercent; }
        public void setFullScorePercent(double fullScorePercent) { this.fullScorePercent = fullScorePercent; }
        public double getZeroScorePercent() { return zeroScorePercent; }
        public void setZeroScorePercent(double zeroScorePercent) { this.zeroScorePercent = zeroScorePercent; }
    }

    public static class EmergencyFund {
        private double fullScoreMonths = 6;
        private double zeroScoreMonths = 0;
        public double getFullScoreMonths() { return fullScoreMonths; }
        public void setFullScoreMonths(double fullScoreMonths) { this.fullScoreMonths = fullScoreMonths; }
        public double getZeroScoreMonths() { return zeroScoreMonths; }
        public void setZeroScoreMonths(double zeroScoreMonths) { this.zeroScoreMonths = zeroScoreMonths; }
    }

    public static class SpendingConsistency {
        private double maxCvPercent = 40;
        private double idealCvPercent = 10;
        public double getMaxCvPercent() { return maxCvPercent; }
        public void setMaxCvPercent(double maxCvPercent) { this.maxCvPercent = maxCvPercent; }
        public double getIdealCvPercent() { return idealCvPercent; }
        public void setIdealCvPercent(double idealCvPercent) { this.idealCvPercent = idealCvPercent; }
    }

    public static class Debt {
        private double zeroScoreDtiPercent = 40;
        private double fullScoreDtiPercent = 10;
        public double getZeroScoreDtiPercent() { return zeroScoreDtiPercent; }
        public void setZeroScoreDtiPercent(double zeroScoreDtiPercent) { this.zeroScoreDtiPercent = zeroScoreDtiPercent; }
        public double getFullScoreDtiPercent() { return fullScoreDtiPercent; }
        public void setFullScoreDtiPercent(double fullScoreDtiPercent) { this.fullScoreDtiPercent = fullScoreDtiPercent; }
    }

    public static class Subscriptions {
        private double highBurdenIncomePercent = 15;
        private double healthyBurdenIncomePercent = 5;
        public double getHighBurdenIncomePercent() { return highBurdenIncomePercent; }
        public void setHighBurdenIncomePercent(double highBurdenIncomePercent) { this.highBurdenIncomePercent = highBurdenIncomePercent; }
        public double getHealthyBurdenIncomePercent() { return healthyBurdenIncomePercent; }
        public void setHealthyBurdenIncomePercent(double healthyBurdenIncomePercent) { this.healthyBurdenIncomePercent = healthyBurdenIncomePercent; }
    }

    public static class Decision {
        private double minEmergencyMonths = 3;
        private int buyConfidenceBase = 70;
        private int waitConfidenceBase = 80;
        private int notRecommendedConfidenceBase = 90;
        public double getMinEmergencyMonths() { return minEmergencyMonths; }
        public void setMinEmergencyMonths(double minEmergencyMonths) { this.minEmergencyMonths = minEmergencyMonths; }
        public int getBuyConfidenceBase() { return buyConfidenceBase; }
        public void setBuyConfidenceBase(int buyConfidenceBase) { this.buyConfidenceBase = buyConfidenceBase; }
        public int getWaitConfidenceBase() { return waitConfidenceBase; }
        public void setWaitConfidenceBase(int waitConfidenceBase) { this.waitConfidenceBase = waitConfidenceBase; }
        public int getNotRecommendedConfidenceBase() { return notRecommendedConfidenceBase; }
        public void setNotRecommendedConfidenceBase(int notRecommendedConfidenceBase) { this.notRecommendedConfidenceBase = notRecommendedConfidenceBase; }
    }

    /** Budget prediction blending weights (must sum to 1.0). */
    public static class Prediction {
        private double currentMonthWeight = 0.70;
        private double historicalWeight = 0.30;
        private int historicalMonths = 6;
        public double getCurrentMonthWeight() { return currentMonthWeight; }
        public void setCurrentMonthWeight(double currentMonthWeight) { this.currentMonthWeight = currentMonthWeight; }
        public double getHistoricalWeight() { return historicalWeight; }
        public void setHistoricalWeight(double historicalWeight) { this.historicalWeight = historicalWeight; }
        public int getHistoricalMonths() { return historicalMonths; }
        public void setHistoricalMonths(int historicalMonths) { this.historicalMonths = historicalMonths; }
    }

    /** Overspend risk bands as % of budget (predicted / budget × 100). */
    public static class Risk {
        private double lowMaxPercent = 70;
        private double mediumMaxPercent = 90;
        private double highMaxPercent = 110;
        public double getLowMaxPercent() { return lowMaxPercent; }
        public void setLowMaxPercent(double lowMaxPercent) { this.lowMaxPercent = lowMaxPercent; }
        public double getMediumMaxPercent() { return mediumMaxPercent; }
        public void setMediumMaxPercent(double mediumMaxPercent) { this.mediumMaxPercent = mediumMaxPercent; }
        public double getHighMaxPercent() { return highMaxPercent; }
        public void setHighMaxPercent(double highMaxPercent) { this.highMaxPercent = highMaxPercent; }
    }

    /** Weighted purchase affordability model (points sum to 100). */
    public static class Affordability {
        private int remainingBudgetWeight = 25;
        private int emergencyFundWeight = 20;
        private int upcomingBillsWeight = 20;
        private int savingsGoalWeight = 15;
        private int overspendingHistoryWeight = 10;
        private int cashFlowStabilityWeight = 10;
        private int buyMinScore = 85;
        private int buyWithCautionMinScore = 70;
        private int waitMinScore = 50;
        private double targetEmergencyMonths = 6;
        private double minEmergencyMonths = 3;
        private int necessityBonus = 5;
        private int luxuryPenalty = 10;
        public int getRemainingBudgetWeight() { return remainingBudgetWeight; }
        public void setRemainingBudgetWeight(int remainingBudgetWeight) { this.remainingBudgetWeight = remainingBudgetWeight; }
        public int getEmergencyFundWeight() { return emergencyFundWeight; }
        public void setEmergencyFundWeight(int emergencyFundWeight) { this.emergencyFundWeight = emergencyFundWeight; }
        public int getUpcomingBillsWeight() { return upcomingBillsWeight; }
        public void setUpcomingBillsWeight(int upcomingBillsWeight) { this.upcomingBillsWeight = upcomingBillsWeight; }
        public int getSavingsGoalWeight() { return savingsGoalWeight; }
        public void setSavingsGoalWeight(int savingsGoalWeight) { this.savingsGoalWeight = savingsGoalWeight; }
        public int getOverspendingHistoryWeight() { return overspendingHistoryWeight; }
        public void setOverspendingHistoryWeight(int overspendingHistoryWeight) { this.overspendingHistoryWeight = overspendingHistoryWeight; }
        public int getCashFlowStabilityWeight() { return cashFlowStabilityWeight; }
        public void setCashFlowStabilityWeight(int cashFlowStabilityWeight) { this.cashFlowStabilityWeight = cashFlowStabilityWeight; }
        public int getBuyMinScore() { return buyMinScore; }
        public void setBuyMinScore(int buyMinScore) { this.buyMinScore = buyMinScore; }
        public int getBuyWithCautionMinScore() { return buyWithCautionMinScore; }
        public void setBuyWithCautionMinScore(int buyWithCautionMinScore) { this.buyWithCautionMinScore = buyWithCautionMinScore; }
        public int getWaitMinScore() { return waitMinScore; }
        public void setWaitMinScore(int waitMinScore) { this.waitMinScore = waitMinScore; }
        public double getTargetEmergencyMonths() { return targetEmergencyMonths; }
        public void setTargetEmergencyMonths(double targetEmergencyMonths) { this.targetEmergencyMonths = targetEmergencyMonths; }
        public double getMinEmergencyMonths() { return minEmergencyMonths; }
        public void setMinEmergencyMonths(double minEmergencyMonths) { this.minEmergencyMonths = minEmergencyMonths; }
        public int getNecessityBonus() { return necessityBonus; }
        public void setNecessityBonus(int necessityBonus) { this.necessityBonus = necessityBonus; }
        public int getLuxuryPenalty() { return luxuryPenalty; }
        public void setLuxuryPenalty(int luxuryPenalty) { this.luxuryPenalty = luxuryPenalty; }
        public int totalWeight() {
            return remainingBudgetWeight + emergencyFundWeight + upcomingBillsWeight
                    + savingsGoalWeight + overspendingHistoryWeight + cashFlowStabilityWeight;
        }
    }
}
