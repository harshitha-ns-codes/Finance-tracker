package com.example.financetracker.transaction;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(transactionService.listForCurrentUser(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id,
                                              @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

