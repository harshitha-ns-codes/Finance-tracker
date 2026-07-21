package com.example.financetracker.advisor;

public class DailyInsightResponse {

    private String type;
    private String insight;
    private String action;
    private String actionRoute;

    public DailyInsightResponse() {
    }

    public DailyInsightResponse(String type, String insight, String action, String actionRoute) {
        this.type = type;
        this.insight = insight;
        this.action = action;
        this.actionRoute = actionRoute;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInsight() {
        return insight;
    }

    public void setInsight(String insight) {
        this.insight = insight;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionRoute() {
        return actionRoute;
    }

    public void setActionRoute(String actionRoute) {
        this.actionRoute = actionRoute;
    }
}
