package com.example.financetracker.streak;

public class StreakMetricDto {

    private StreakType type;
    private String label;
    private int current;
    private int best;
    private String unit;
    private boolean broken;
    private String brokenMessage;
    private boolean atRisk;

    public StreakType getType() {
        return type;
    }

    public void setType(StreakType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getBest() {
        return best;
    }

    public void setBest(int best) {
        this.best = best;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public String getBrokenMessage() {
        return brokenMessage;
    }

    public void setBrokenMessage(String brokenMessage) {
        this.brokenMessage = brokenMessage;
    }

    public boolean isAtRisk() {
        return atRisk;
    }

    public void setAtRisk(boolean atRisk) {
        this.atRisk = atRisk;
    }
}
