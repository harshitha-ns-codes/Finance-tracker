package com.example.financetracker.split;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/splits")
public class BillSplitController {

    private final BillSplitService billSplitService;

    public BillSplitController(BillSplitService billSplitService) {
        this.billSplitService = billSplitService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BillSplitRequest request) {
        try {
            return ResponseEntity.ok(billSplitService.create(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<BillSplitDto>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(billSplitService.list(status));
    }

    @GetMapping("/summary")
    public ResponseEntity<BillSplitSummaryDto> summary() {
        return ResponseEntity.ok(billSplitService.summary());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BillSplitStatusRequest request) {
        try {
            return ResponseEntity.ok(billSplitService.updateStatus(id, request.getStatus()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            billSplitService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
