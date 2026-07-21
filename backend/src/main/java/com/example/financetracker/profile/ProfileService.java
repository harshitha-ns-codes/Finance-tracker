package com.example.financetracker.profile;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.user.User;
import com.example.financetracker.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ProfileService(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public SalaryProfileResponse updateSalary(SalaryUpdateRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setSalaryDay(request.getSalaryDay());
        user.setSalaryAmount(BigDecimal.valueOf(request.getSalaryAmount()));
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public SalaryProfileResponse getSalaryProfile(User user) {
        return toResponse(user);
    }

    public static SalaryProfileResponse toResponse(User user) {
        boolean configured = user.getSalaryDay() != null
                && user.getSalaryAmount() != null
                && user.getSalaryAmount().compareTo(BigDecimal.ZERO) > 0;
        Double amount = configured ? user.getSalaryAmount().doubleValue() : null;
        return new SalaryProfileResponse(user.getSalaryDay(), amount, configured);
    }
}
