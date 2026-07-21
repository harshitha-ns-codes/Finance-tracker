package com.example.financetracker.health.model;

import jakarta.persistence.*;
import java.time.YearMonth;

@Entity
@Table(name = "health_score_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "score_month"}))
public class HealthScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "score_month", nullable = false, length = 7)
    private String scoreMonth; // yyyy-MM

    @Column(nullable = false)
    private int score;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getScoreMonth() { return scoreMonth; }
    public void setScoreMonth(String scoreMonth) { this.scoreMonth = scoreMonth; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public YearMonth asYearMonth() {
        return YearMonth.parse(scoreMonth);
    }
}
