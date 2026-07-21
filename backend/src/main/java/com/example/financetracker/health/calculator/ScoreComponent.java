package com.example.financetracker.health.calculator;

import com.example.financetracker.health.dto.CategoryScoreDto;

@FunctionalInterface
public interface ScoreComponent {
    CategoryScoreDto calculate(HealthScoreContext ctx);
}
