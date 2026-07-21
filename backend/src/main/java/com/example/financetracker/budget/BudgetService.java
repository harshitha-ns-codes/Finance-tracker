package com.example.financetracker.budget;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CurrentUserService currentUserService;

    public BudgetService(BudgetRepository budgetRepository,
                         CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Budget upsertBudget(BudgetRequest request) {
        User user = currentUserService.getCurrentUser();
        YearMonth month;
        try {
            month = YearMonth.parse(request.getMonth());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }

        Budget budget = budgetRepository.findByUserAndMonth(user, month)
                .orElseGet(Budget::new);
        budget.setUser(user);
        budget.setMonth(month);
        budget.setMonthlyLimit(request.getMonthlyLimit());
        return budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public Budget getBudgetForMonth(YearMonth month) {
        User user = currentUserService.getCurrentUser();
        return budgetRepository.findByUserAndMonth(user, month)
                .orElse(null);
    }
}

