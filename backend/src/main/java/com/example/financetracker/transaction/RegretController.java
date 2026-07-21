package com.example.financetracker.transaction;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class RegretController {

    private final RegretService regretService;

    public RegretController(RegretService regretService) {
        this.regretService = regretService;
    }

    @GetMapping("/regret/pending")
    public ResponseEntity<List<PendingRegretDto>> pending() {
        return ResponseEntity.ok(regretService.pendingReviews());
    }

    @GetMapping("/regret/stats")
    public ResponseEntity<RegretStatsResponse> stats() {
        return ResponseEntity.ok(regretService.stats());
    }

    @PatchMapping("/{id}/regret")
    public ResponseEntity<?> updateRegret(
            @PathVariable Long id,
            @Valid @RequestBody RegretUpdateRequest request) {
        try {
            return ResponseEntity.ok(regretService.updateRegret(id, request.getStatus()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
