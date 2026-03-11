package com.example.financetracker.analytics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummary> dashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalyDto>> anomalies() {
        return ResponseEntity.ok(analyticsService.detectAnomalies());
    }
}

