package com.example.financetracker.networth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/net-worth")
public class NetWorthController {

    private final NetWorthService netWorthService;

    public NetWorthController(NetWorthService netWorthService) {
        this.netWorthService = netWorthService;
    }

    @GetMapping
    public ResponseEntity<NetWorthResponse> get() {
        return ResponseEntity.ok(netWorthService.getNetWorth());
    }

    @PutMapping
    public ResponseEntity<NetWorthResponse> save(@Valid @RequestBody NetWorthUpdateRequest request) {
        return ResponseEntity.ok(netWorthService.saveNetWorth(request));
    }
}
