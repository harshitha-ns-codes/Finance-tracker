package com.example.financetracker.forecast;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final CashFlowForecastService cashFlowForecastService;
    private final SalaryIntelligenceService salaryIntelligenceService;

    public ForecastController(
            CashFlowForecastService cashFlowForecastService,
            SalaryIntelligenceService salaryIntelligenceService) {
        this.cashFlowForecastService = cashFlowForecastService;
        this.salaryIntelligenceService = salaryIntelligenceService;
    }

    @GetMapping("/salary-intelligence")
    public ResponseEntity<SalaryIntelligenceResponse> salaryIntelligence() {
        return ResponseEntity.ok(salaryIntelligenceService.getSalaryIntelligence());
    }

    @GetMapping("/cashflow")
    public ResponseEntity<?> cashFlow(@RequestParam(required = false) String month) {
        YearMonth ym;
        try {
            ym = (month == null || month.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(month);
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid month format. Expected yyyy-MM"));
        }
        return ResponseEntity.ok(cashFlowForecastService.forecast(ym));
    }
}
