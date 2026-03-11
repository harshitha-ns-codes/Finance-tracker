package com.example.financetracker.analytics;

import com.example.financetracker.budget.Budget;
import com.example.financetracker.budget.BudgetRepository;
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
import java.util.List;

@Service
public class AnalyticsService {

    private static final BigDecimal BUDGET_ALERT_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal LARGE_TRANSACTION_MULTIPLIER = new BigDecimal("2.5");

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository,
                            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        User user = currentUserService.getCurrentUser();

        BigDecimal income = transactionRepository.totalIncomeForUser(user);
        BigDecimal expenses = transactionRepository.totalExpensesForUser(user);

        DashboardSummary summary = new DashboardSummary();
        summary.setTotalIncome(income);
        summary.setTotalExpenses(expenses);
        summary.setBalance(income.subtract(expenses));

        List<Object[]> topCats = transactionRepository.topSpendingCategories(user);
        if (!topCats.isEmpty()) {
            Object[] row = topCats.get(0);
            summary.setTopSpendingCategory((String) row[0]);
            summary.setTopSpendingAmount((BigDecimal) row[1]);
        }

        YearMonth currentMonth = YearMonth.now();
        Budget budget = budgetRepository.findByUserAndMonth(user, currentMonth).orElse(null);
        if (budget != null) {
            summary.setMonthlyBudgetLimit(budget.getMonthlyLimit());
            LocalDate start = currentMonth.atDay(1);
            LocalDate end = currentMonth.atEndOfMonth();
            List<Transaction> monthTxs = transactionRepository.findByUserAndDateBetween(user, start, end);
            BigDecimal monthExpenses = monthTxs.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setMonthlyExpenses(monthExpenses);
            if (budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = monthExpenses
                        .divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP);
                summary.setNearBudgetLimit(ratio.compareTo(BUDGET_ALERT_THRESHOLD) >= 0);
            }
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
}

