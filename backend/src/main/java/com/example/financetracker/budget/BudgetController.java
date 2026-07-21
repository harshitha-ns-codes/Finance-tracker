package com.example.financetracker.budget;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryBudgetService categoryBudgetService;

    public BudgetController(BudgetService budgetService,
                            CategoryBudgetService categoryBudgetService) {
        this.budgetService = budgetService;
        this.categoryBudgetService = categoryBudgetService;
    }

    @PostMapping
    public ResponseEntity<Budget> upsert(@Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.upsertBudget(request));
    }

    @GetMapping("/{month}")
    public ResponseEntity<?> getForMonth(@PathVariable String month) {
        Budget budget;
        try {
            budget = budgetService.getBudgetForMonth(java.time.YearMonth.parse(month));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid month format. Expected yyyy-MM"));
        }
        if (budget == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Budget not found"));
        }
        return ResponseEntity.ok(budget);
    }

    @GetMapping("/{month}/items")
    public ResponseEntity<List<CategoryBudgetItemView>> listItems(@PathVariable String month) {
        return ResponseEntity.ok(categoryBudgetService.listForMonth(month));
    }

    @PostMapping("/items")
    public ResponseEntity<CategoryBudgetItemView> createItem(
            @Valid @RequestBody CategoryBudgetItemRequest request) {
        return ResponseEntity.ok(categoryBudgetService.create(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CategoryBudgetItemView> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CategoryBudgetItemRequest request) {
        return ResponseEntity.ok(categoryBudgetService.update(id, request));
    }

    @PutMapping("/items/{id}/paid")
    public ResponseEntity<CategoryBudgetItemView> setPaid(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean paid = Boolean.TRUE.equals(body.get("paid"));
        return ResponseEntity.ok(categoryBudgetService.setPaid(id, paid));
    }

    @PutMapping("/spent")
    public ResponseEntity<List<CategoryBudgetItemView>> setCategorySpent(
            @Valid @RequestBody CategorySpentRequest request) {
        return ResponseEntity.ok(categoryBudgetService.setCategorySpent(
                request.getMonth(), request.getCategory(), request.getSpentAmount()));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        categoryBudgetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
