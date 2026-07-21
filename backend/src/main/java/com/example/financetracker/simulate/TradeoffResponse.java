package com.example.financetracker.simulate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TradeoffResponse {

    private TradeoffOptionResult option1;
    private TradeoffOptionResult option2;
    private String recommendation; // option1 | option2
    private String recommendationReason;

    public TradeoffOptionResult getOption1() {
        return option1;
    }

    public void setOption1(TradeoffOptionResult option1) {
        this.option1 = option1;
    }

    public TradeoffOptionResult getOption2() {
        return option2;
    }

    public void setOption2(TradeoffOptionResult option2) {
        this.option2 = option2;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public static class TradeoffOptionResult {
        private String name;
        private String type;
        private BigDecimal immediateBalanceImpact;
        private BigDecimal monthlyImpact;
        private int healthScoreImpact;
        private String timeToRecover;
        private List<String> pros = new ArrayList<>();
        private List<String> cons = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getImmediateBalanceImpact() {
            return immediateBalanceImpact;
        }

        public void setImmediateBalanceImpact(BigDecimal immediateBalanceImpact) {
            this.immediateBalanceImpact = immediateBalanceImpact;
        }

        public BigDecimal getMonthlyImpact() {
            return monthlyImpact;
        }

        public void setMonthlyImpact(BigDecimal monthlyImpact) {
            this.monthlyImpact = monthlyImpact;
        }

        public int getHealthScoreImpact() {
            return healthScoreImpact;
        }

        public void setHealthScoreImpact(int healthScoreImpact) {
            this.healthScoreImpact = healthScoreImpact;
        }

        public String getTimeToRecover() {
            return timeToRecover;
        }

        public void setTimeToRecover(String timeToRecover) {
            this.timeToRecover = timeToRecover;
        }

        public List<String> getPros() {
            return pros;
        }

        public void setPros(List<String> pros) {
            this.pros = pros;
        }

        public List<String> getCons() {
            return cons;
        }

        public void setCons(List<String> cons) {
            this.cons = cons;
        }
    }
}
