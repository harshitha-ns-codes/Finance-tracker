package com.example.financetracker.goal;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    public FinancialGoalController(FinancialGoalService financialGoalService) {
        this.financialGoalService = financialGoalService;
    }

    @GetMapping
    public ResponseEntity<List<FinancialGoalDto>> list() {
        return ResponseEntity.ok(financialGoalService.listGoals());
    }

    @GetMapping("/emergency-recommendation")
    public ResponseEntity<EmergencyFundRecommendationDto> emergencyRecommendation() {
        return ResponseEntity.ok(financialGoalService.emergencyRecommendation());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody FinancialGoalCreateRequest request) {
        try {
            return ResponseEntity.ok(financialGoalService.createGoal(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<?> contribute(
            @PathVariable Long id,
            @Valid @RequestBody GoalContributeRequest request) {
        try {
            return ResponseEntity.ok(financialGoalService.contribute(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        financialGoalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
}
