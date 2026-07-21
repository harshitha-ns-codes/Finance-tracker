package com.example.financetracker.health.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryScoreDto {
    private String category;
    private int score;
    private int max;
    private String explanation;
    private List<String> details = new ArrayList<>();

    public CategoryScoreDto() {}

    public CategoryScoreDto(String category, int score, int max, String explanation) {
        this.category = category;
        this.score = score;
        this.max = max;
        this.explanation = explanation;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
