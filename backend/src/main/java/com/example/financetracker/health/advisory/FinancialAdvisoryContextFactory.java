package com.example.financetracker.health.advisory;

import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.PlannedPurchase;
import com.example.financetracker.health.repo.PlannedPurchaseRepository;
import com.example.financetracker.health.service.HealthScoreContextFactory;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class FinancialAdvisoryContextFactory {

    private final HealthScoreContextFactory healthContextFactory;
    private final PlannedPurchaseRepository plannedPurchaseRepository;
    private final CategoryBudgetService categoryBudgetService;

    public FinancialAdvisoryContextFactory(
            HealthScoreContextFactory healthContextFactory,
            PlannedPurchaseRepository plannedPurchaseRepository,
            CategoryBudgetService categoryBudgetService) {
        this.healthContextFactory = healthContextFactory;
        this.plannedPurchaseRepository = plannedPurchaseRepository;
        this.categoryBudgetService = categoryBudgetService;
    }

    @Transactional(readOnly = true)
    public FinancialAdvisoryContext build(User user, YearMonth month) {
        return build(user, month, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public FinancialAdvisoryContext build(User user, YearMonth month, LocalDate asOfDate) {
        HealthScoreContext health = healthContextFactory.build(user, month);

        Map<String, BigDecimal> categoryBudgets = new LinkedHashMap<>();
        for (var item : health.getBudgetItems()) {
            categoryBudgets.merge(item.getCategory(), item.getPlannedAmount(), BigDecimal::add);
        }

        int historyMonths = 6;
        Map<String, List<BigDecimal>> historical = buildHistoricalCategorySpend(user, month, historyMonths);

        FinancialProfile profile = health.getProfile();
        BigDecimal balance = profile != null ? profile.getCurrentBalance() : BigDecimal.ZERO;
        int salaryDay = profile != null && profile.getSalaryDayOfMonth() != null
                ? profile.getSalaryDayOfMonth() : 1;

        List<PlannedPurchase> planned = plannedPurchaseRepository.findByUserIdAndActiveTrue(user.getId());

        return new FinancialAdvisoryContext(
                health, asOfDate, categoryBudgets, historical, balance, salaryDay, planned);
    }

    private Map<String, List<BigDecimal>> buildHistoricalCategorySpend(
            User user, YearMonth currentMonth, int months) {
        Map<String, List<BigDecimal>> result = new LinkedHashMap<>();
        YearMonth start = currentMonth.minusMonths(months - 1);

        for (YearMonth ym = start; !ym.isAfter(currentMonth); ym = ym.plusMonths(1)) {
            Map<String, BigDecimal> monthCat =
                    categoryBudgetService.effectiveSpentByCategory(user, ym);
            for (Map.Entry<String, BigDecimal> e : monthCat.entrySet()) {
                result.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
            }
        }
        return result;
    }
}
