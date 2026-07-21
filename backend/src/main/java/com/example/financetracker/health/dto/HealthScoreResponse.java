package com.example.financetracker.health.dto;

import java.util.ArrayList;
import java.util.List;

public class HealthScoreResponse {
    private int score;
    private String rating;
    private Integer monthDelta;
    private List<CategoryScoreDto> breakdown = new ArrayList<>();
    private List<String> positives = new ArrayList<>();
    private List<String> negatives = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public Integer getMonthDelta() { return monthDelta; }
    public void setMonthDelta(Integer monthDelta) { this.monthDelta = monthDelta; }
    public List<CategoryScoreDto> getBreakdown() { return breakdown; }
    public void setBreakdown(List<CategoryScoreDto> breakdown) { this.breakdown = breakdown; }
    public List<String> getPositives() { return positives; }
    public void setPositives(List<String> positives) { this.positives = positives; }
    public List<String> getNegatives() { return negatives; }
    public void setNegatives(List<String> negatives) { this.negatives = negatives; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
