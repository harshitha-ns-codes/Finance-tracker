package com.example.financetracker.forecast;

import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.recurring.RecurringTransaction;
import com.example.financetracker.recurring.RecurringTransactionRepository;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.transaction.TransactionRepository;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SalaryIntelligenceService {

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final CurrentUserService currentUserService;
    private final CategoryBudgetService categoryBudgetService;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringRepository;

    public SalaryIntelligenceService(
            CurrentUserService currentUserService,
            CategoryBudgetService categoryBudgetService,
            TransactionRepository transactionRepository,
            RecurringTransactionRepository recurringRepository) {
        this.currentUserService = currentUserService;
        this.categoryBudgetService = categoryBudgetService;
        this.transactionRepository = transactionRepository;
        this.recurringRepository = recurringRepository;
    }

    @Transactional(readOnly = true)
    public SalaryIntelligenceResponse getSalaryIntelligence() {
        User user = currentUserService.getCurrentUser();
        if (user.getSalaryDay() == null
                || user.getSalaryAmount() == null
                || user.getSalaryAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return SalaryIntelligenceResponse.notConfigured();
        }

        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        int salaryDay = Math.min(Math.max(user.getSalaryDay(), 1), month.lengthOfMonth());
        int todayDay = today.getDayOfMonth();
        int daysInMonth = month.lengthOfMonth();

        String zone;
        int daysUntilSalary;
        if (todayDay < salaryDay) {
            daysUntilSalary = salaryDay - todayDay;
            zone = "PRE_SALARY";
        } else {
            daysUntilSalary = salaryDay + daysInMonth - todayDay;
            zone = "POST_SALARY";
        }

        double salaryAmount = user.getSalaryAmount().doubleValue();
        double currentBalance = monthBalance(user, month).doubleValue();

        double dailyBudget;
        if ("PRE_SALARY".equals(zone)) {
            dailyBudget = daysUntilSalary > 0
                    ? round(currentBalance / daysUntilSalary)
                    : round(currentBalance);
        } else {
            dailyBudget = round(salaryAmount / daysInMonth);
        }

        boolean showAllocation = "POST_SALARY".equals(zone) && isNearSalaryDay(todayDay, salaryDay);
        AllocationPlanDto allocationPlan = showAllocation
                ? buildAllocationPlan(user, salaryAmount)
                : null;

        SalaryIntelligenceResponse response = new SalaryIntelligenceResponse();
        response.setConfigured(true);
        response.setZone(zone);
        response.setDaysUntilSalary(daysUntilSalary);
        response.setSalaryDay(salaryDay);
        response.setSalaryAmount(salaryAmount);
        response.setCurrentBalance(round(currentBalance));
        response.setDailyBudget(dailyBudget);
        response.setShowAllocation(showAllocation);
        response.setAllocationPlan(allocationPlan);
        response.setDaysInMonth(daysInMonth);
        response.setTodayDayOfMonth(todayDay);
        response.setMonthLabel(month.format(MONTH_LABEL));
        return response;
    }

    private BigDecimal monthBalance(User user, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Transaction> txs = transactionRepository.findByUserAndDateBetween(user, from, to);
        BigDecimal income = txs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, month);
        return income.subtract(expenses);
    }

    private boolean isNearSalaryDay(int todayDay, int salaryDay) {
        return todayDay == salaryDay
                || todayDay == salaryDay + 1
                || (salaryDay > 1 && todayDay == salaryDay - 1);
    }

    private AllocationPlanDto buildAllocationPlan(User user, double salaryAmount) {
        List<RecurringTransaction> recurring =
                recurringRepository.findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(user);

        double fixedCosts = 0;
        List<AllocationBreakdownItemDto> breakdown = new ArrayList<>();
        for (RecurringTransaction item : recurring) {
            if (item.getType() != TransactionType.EXPENSE) {
                continue;
            }
            double amount = item.getAmount().doubleValue();
            fixedCosts += amount;
            breakdown.add(new AllocationBreakdownItemDto(item.getName(), round(amount), "fixed"));
        }

        double suggestedSavings = round(salaryAmount * 0.20);
        double remainingAfterFixed = salaryAmount - fixedCosts;
        double discretionary = round(remainingAfterFixed - suggestedSavings);

        breakdown.add(new AllocationBreakdownItemDto(
                "Savings (20%)", suggestedSavings, "savings"));
        breakdown.add(new AllocationBreakdownItemDto(
                "Discretionary", discretionary, "discretionary"));

        AllocationPlanDto plan = new AllocationPlanDto();
        plan.setSalaryAmount(round(salaryAmount));
        plan.setFixedCosts(round(fixedCosts));
        plan.setSuggestedSavings(suggestedSavings);
        plan.setDiscretionary(discretionary);
        plan.setBreakdown(breakdown);
        return plan;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
