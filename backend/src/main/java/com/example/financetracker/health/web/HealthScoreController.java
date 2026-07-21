package com.example.financetracker.health.web;

import com.example.financetracker.health.advisory.dto.FinancialAdvisoryResponse;
import com.example.financetracker.health.advisory.dto.PurchaseEvaluateDetailRequest;
import com.example.financetracker.health.advisory.dto.SimulationRequest;
import com.example.financetracker.health.advisory.service.FinancialAdvisoryService;
import com.example.financetracker.health.dto.HealthScoreResponse;
import com.example.financetracker.health.dto.PurchaseEvaluateRequest;
import com.example.financetracker.health.service.HealthScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-score")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;
    private final FinancialAdvisoryService financialAdvisoryService;

    public HealthScoreController(
            HealthScoreService healthScoreService,
            FinancialAdvisoryService financialAdvisoryService) {
        this.healthScoreService = healthScoreService;
        this.financialAdvisoryService = financialAdvisoryService;
    }

    @GetMapping
    public ResponseEntity<HealthScoreResponse> score(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(healthScoreService.getScore(month));
    }

    @PostMapping("/evaluate-purchase")
    public ResponseEntity<FinancialAdvisoryResponse> evaluatePurchase(
            @RequestBody PurchaseEvaluateRequest request) {
        PurchaseEvaluateDetailRequest detail = new PurchaseEvaluateDetailRequest();
        detail.setPrice(request.getPrice());
        detail.setLabel(request.getLabel());
        detail.setCategory(request.getCategory());
        detail.setPriority(request.getPriority() != null ? request.getPriority() : "WANT");
        return ResponseEntity.ok(financialAdvisoryService.evaluatePurchase(detail));
    }

    @PostMapping("/simulate")
    public ResponseEntity<FinancialAdvisoryResponse> simulateFlat(
            @RequestParam(required = false) String month,
            @RequestBody List<SimulationRequest> scenarios) {
        return ResponseEntity.ok(financialAdvisoryService.simulate(month, scenarios));
    }

    /** Full advisory: budget prediction + recommendations. */
    @GetMapping("/advisory")
    public ResponseEntity<FinancialAdvisoryResponse> advisory(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(financialAdvisoryService.getAdvisory(month));
    }

    /** Weighted affordability score + financial simulation for a purchase. */
    @PostMapping("/advisory/evaluate")
    public ResponseEntity<FinancialAdvisoryResponse> evaluateDetailed(
            @RequestBody PurchaseEvaluateDetailRequest request) {
        return ResponseEntity.ok(financialAdvisoryService.evaluatePurchase(request));
    }

    /** What-if scenario analysis. */
    @PostMapping("/advisory/simulate")
    public ResponseEntity<FinancialAdvisoryResponse> simulate(
            @RequestParam(required = false) String month,
            @RequestBody List<SimulationRequest> scenarios) {
        return ResponseEntity.ok(financialAdvisoryService.simulate(month, scenarios));
    }
}
