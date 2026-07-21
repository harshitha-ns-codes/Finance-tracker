package com.example.financetracker.goal;

public class GoalMilestoneDto {

    private int percent;
    private boolean achieved;
    private String message;

    public GoalMilestoneDto() {
    }

    public GoalMilestoneDto(int percent, boolean achieved, String message) {
        this.percent = percent;
        this.achieved = achieved;
        this.message = message;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public void setAchieved(boolean achieved) {
        this.achieved = achieved;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
