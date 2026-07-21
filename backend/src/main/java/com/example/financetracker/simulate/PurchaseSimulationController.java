package com.example.financetracker.simulate;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/simulate")
public class PurchaseSimulationController {

    private final PurchaseSimulationService purchaseSimulationService;
    private final TradeoffService tradeoffService;

    public PurchaseSimulationController(
            PurchaseSimulationService purchaseSimulationService,
            TradeoffService tradeoffService) {
        this.purchaseSimulationService = purchaseSimulationService;
        this.tradeoffService = tradeoffService;
    }

    @PostMapping("/purchase")
    public ResponseEntity<?> simulatePurchase(@Valid @RequestBody PurchaseSimulationRequest request) {
        try {
            return ResponseEntity.ok(purchaseSimulationService.simulate(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/tradeoff")
    public ResponseEntity<?> simulateTradeoff(@Valid @RequestBody TradeoffRequest request) {
        try {
            return ResponseEntity.ok(tradeoffService.compare(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
