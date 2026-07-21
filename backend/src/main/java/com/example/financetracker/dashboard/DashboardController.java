package com.example.financetracker.dashboard;

import com.example.financetracker.analytics.AnalyticsService;
import com.example.financetracker.analytics.CategoryBreakdownDto;
import com.example.financetracker.analytics.MonthlyTrendDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AnalyticsService analyticsService;

    public DashboardController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/trends")
    public ResponseEntity<List<MonthlyTrendDto>> trends(
            @RequestParam(defaultValue = "6") int months) {
        int clamped = Math.max(1, Math.min(months, 24));
        return ResponseEntity.ok(analyticsService.getMonthlyTrends(clamped));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryBreakdownDto>> categories(
            @RequestParam(required = false) String month) {
        YearMonth ym;
        try {
            ym = (month == null || month.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(month);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(ym));
    }
}
