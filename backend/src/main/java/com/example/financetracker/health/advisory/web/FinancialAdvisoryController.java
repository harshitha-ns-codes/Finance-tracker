package com.example.financetracker.health.advisory.web;

import com.example.financetracker.health.advisory.dto.FinancialAdvisoryResponse;
import com.example.financetracker.health.advisory.dto.PurchaseEvaluateDetailRequest;
import com.example.financetracker.health.advisory.dto.SimulationRequest;
import com.example.financetracker.health.advisory.service.FinancialAdvisoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-advisory")
public class FinancialAdvisoryController {

    private final FinancialAdvisoryService advisoryService;

    public FinancialAdvisoryController(FinancialAdvisoryService advisoryService) {
        this.advisoryService = advisoryService;
    }

    @GetMapping
    public ResponseEntity<FinancialAdvisoryResponse> advisory(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(advisoryService.getAdvisory(month));
    }

    @PostMapping("/evaluate-purchase")
    public ResponseEntity<FinancialAdvisoryResponse> evaluatePurchase(
            @RequestBody PurchaseEvaluateDetailRequest request) {
        return ResponseEntity.ok(advisoryService.evaluatePurchase(request));
    }

    @PostMapping("/simulate")
    public ResponseEntity<FinancialAdvisoryResponse> simulate(
            @RequestParam(required = false) String month,
            @RequestBody List<SimulationRequest> scenarios) {
        return ResponseEntity.ok(advisoryService.simulate(month, scenarios));
    }
}
