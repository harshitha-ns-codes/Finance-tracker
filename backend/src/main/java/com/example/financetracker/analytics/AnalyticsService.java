package com.example.financetracker.analytics;

import com.example.financetracker.budget.Budget;
import com.example.financetracker.budget.BudgetRepository;
import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final BigDecimal BUDGET_ALERT_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal LARGE_TRANSACTION_MULTIPLIER = new BigDecimal("2.5");

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryBudgetService categoryBudgetService;
    private final CurrentUserService currentUserService;

    public AnalyticsService(TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository,
                            CategoryBudgetService categoryBudgetService,
                            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.categoryBudgetService = categoryBudgetService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        User user = currentUserService.getCurrentUser();
        YearMonth currentMonth = YearMonth.now();

        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        List<Transaction> monthTxs =
                transactionRepository.findByUserAndDateBetween(user, monthStart, monthEnd);
        BigDecimal monthlyIncome = monthTxs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Same spent total as Budget sheet + Health score
        BigDecimal monthlyExpenses = categoryBudgetService.totalEffectiveSpent(user, currentMonth);
        Map<String, BigDecimal> currentMonthSpent =
                categoryBudgetService.effectiveSpentByCategory(user, currentMonth);

        DashboardSummary summary = new DashboardSummary();
        summary.setMonth(currentMonth.toString());
        summary.setMonthlyIncome(monthlyIncome);
        summary.setMonthlyExpenses(monthlyExpenses);
        // Primary KPIs = this month (keeps Overview in sync with Budget / Health)
        summary.setTotalIncome(monthlyIncome);
        summary.setTotalExpenses(monthlyExpenses);
        summary.setBalance(monthlyIncome.subtract(monthlyExpenses));

        if (!currentMonthSpent.isEmpty()) {
            Map.Entry<String, BigDecimal> top = currentMonthSpent.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (top != null && top.getValue().compareTo(BigDecimal.ZERO) > 0) {
                summary.setTopSpendingCategory(top.getKey());
                summary.setTopSpendingAmount(top.getValue());
            }
        }

        Budget budget = budgetRepository.findByUserAndMonth(user, currentMonth).orElse(null);
        BigDecimal monthlyLimit = budget != null ? budget.getMonthlyLimit() : BigDecimal.ZERO;
        if (monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            monthlyLimit = categoryBudgetService.totalPlannedForMonth(user, currentMonth);
        }
        if (monthlyLimit.compareTo(BigDecimal.ZERO) > 0) {
            summary.setMonthlyBudgetLimit(monthlyLimit);
            BigDecimal ratio = monthlyExpenses.divide(monthlyLimit, 4, RoundingMode.HALF_UP);
            summary.setNearBudgetLimit(ratio.compareTo(BUDGET_ALERT_THRESHOLD) >= 0);
        }

        return summary;
    }

    @Transactional(readOnly = true)
    public List<AnomalyDto> detectAnomalies() {
        User user = currentUserService.getCurrentUser();
        List<Transaction> all = transactionRepository.findByUser(user);
        if (all.isEmpty()) {
            return List.of();
        }

        List<Transaction> expenses = all.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .toList();

        if (expenses.isEmpty()) {
            return List.of();
        }

        BigDecimal avg = expenses.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(expenses.size()), 2, RoundingMode.HALF_UP);

        List<AnomalyDto> anomalies = new ArrayList<>();
        for (Transaction t : expenses) {
            if (t.getAmount().compareTo(avg.multiply(LARGE_TRANSACTION_MULTIPLIER)) > 0) {
                AnomalyDto dto = new AnomalyDto();
                dto.setTransactionId(t.getId());
                dto.setAmount(t.getAmount());
                dto.setCategory(t.getCategory());
                dto.setDate(t.getDate());
                dto.setReason("Unusually large expense vs average");
                anomalies.add(dto);
            }
        }

        anomalies.sort(Comparator.comparing(AnomalyDto::getDate).reversed());
        return anomalies;
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendDto> getMonthlyTrends(int months) {
        User user = currentUserService.getCurrentUser();
        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(months - 1L);
        LocalDate fromDate = start.atDay(1);

        List<Object[]> rows = transactionRepository.monthlyTotalsGrouped(user.getId(), fromDate);
        Map<YearMonth, BigDecimal> incomeByMonth = new HashMap<>();

        for (Object[] row : rows) {
            int year = toInt(row[0]);
            int month = toInt(row[1]);
            YearMonth ym = YearMonth.of(year, month);
            incomeByMonth.put(ym, toBigDecimal(row[2]));
        }

        List<MonthlyTrendDto> result = new ArrayList<>(months);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            BigDecimal income = incomeByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, ym);
            result.add(new MonthlyTrendDto(ym.toString(), income, expenses));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getCategoryBreakdown(YearMonth month) {
        User user = currentUserService.getCurrentUser();
        Map<String, BigDecimal> spentByCategory =
                categoryBudgetService.effectiveSpentByCategory(user, month);
        BigDecimal total = spentByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownDto> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : spentByCategory.entrySet()) {
            BigDecimal amount = entry.getValue();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            double percentage = 0;
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount
                        .multiply(new BigDecimal("100"))
                        .divide(total, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            result.add(new CategoryBreakdownDto(entry.getKey(), amount, percentage));
        }
        result.sort(Comparator.comparing(CategoryBreakdownDto::getAmount).reversed());
        return result;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}

