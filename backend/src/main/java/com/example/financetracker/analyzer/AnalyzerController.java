package com.example.financetracker.analyzer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analyzer")
public class AnalyzerController {

    private final Analyzer502030Service analyzer502030Service;

    public AnalyzerController(Analyzer502030Service analyzer502030Service) {
        this.analyzer502030Service = analyzer502030Service;
    }

    @GetMapping("/502030")
    public ResponseEntity<?> analyze(@RequestParam(required = false) String month) {
        String ym = month;
        if (ym == null || ym.isBlank()) {
            ym = java.time.YearMonth.now().toString();
        }
        try {
            return ResponseEntity.ok(analyzer502030Service.analyze(ym));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid month format. Expected yyyy-MM"));
        }
    }
}
