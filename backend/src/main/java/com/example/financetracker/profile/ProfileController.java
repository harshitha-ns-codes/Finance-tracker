package com.example.financetracker.profile;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PutMapping("/salary")
    public ResponseEntity<SalaryProfileResponse> updateSalary(@Valid @RequestBody SalaryUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateSalary(request));
    }
}
