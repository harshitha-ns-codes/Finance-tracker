package com.example.financetracker.budget;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<Budget> upsert(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.upsertBudget(request));
    }

    @GetMapping("/{month}")
    public ResponseEntity<Budget> getForMonth(@PathVariable String month) {
        Budget budget;
        try {
            budget = budgetService.getBudgetForMonth(java.time.YearMonth.parse(month));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
        if (budget == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(budget);
    }
}

