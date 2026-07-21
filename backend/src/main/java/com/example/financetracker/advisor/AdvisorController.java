package com.example.financetracker.advisor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

    private final DailyAdvisorService dailyAdvisorService;

    public AdvisorController(DailyAdvisorService dailyAdvisorService) {
        this.dailyAdvisorService = dailyAdvisorService;
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyInsightResponse> daily() {
        return ResponseEntity.ok(dailyAdvisorService.todaysInsight());
    }
}
