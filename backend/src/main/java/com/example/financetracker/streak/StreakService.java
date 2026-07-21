package com.example.financetracker.streak;

import com.example.financetracker.budget.Budget;
import com.example.financetracker.budget.BudgetRepository;
import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.budget.CategoryBudgetItemRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class StreakService {

    private static final double SAVINGS_RATE_THRESHOLD = 20.0;
    private static final int HEATMAP_DAYS = 84;

    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;
    private final CategoryBudgetService categoryBudgetService;
    private final CategoryBudgetItemRepository budgetItemRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringRepository;

    public StreakService(
            CurrentUserService currentUserService,
            TransactionRepository transactionRepository,
            CategoryBudgetService categoryBudgetService,
            CategoryBudgetItemRepository budgetItemRepository,
            BudgetRepository budgetRepository,
            RecurringTransactionRepository recurringRepository) {
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
        this.categoryBudgetService = categoryBudgetService;
        this.budgetItemRepository = budgetItemRepository;
        this.budgetRepository = budgetRepository;
        this.recurringRepository = recurringRepository;
    }

    @Transactional(readOnly = true)
    public StreaksResponse getStreaks() {
        User user = currentUserService.getCurrentUser();
        LocalDate today = LocalDate.now();
        List<Transaction> allTx = transactionRepository.findByUser(user);
        List<CategoryBudgetItem> allBudgetItems = budgetItemRepository.findByUser(user);

        Set<LocalDate> loggedDates = new TreeSet<>();
        Map<LocalDate, BigDecimal> dailyExpense = new HashMap<>();
        Map<LocalDate, Integer> dailyLogCount = new HashMap<>();

        for (Transaction tx : allTx) {
            if (tx.getDate() == null) {
                continue;
            }
            loggedDates.add(tx.getDate());
            dailyLogCount.merge(tx.getDate(), 1, Integer::sum);
            if (tx.getType() == TransactionType.EXPENSE) {
                dailyExpense.merge(tx.getDate(), tx.getAmount(), BigDecimal::add);
            }
        }

        StreaksResponse response = new StreaksResponse();
        response.getStreaks().add(computeLoggingStreak(loggedDates, today));
        response.getStreaks().add(computeUnderBudgetStreak(user, dailyExpense, today));
        response.getStreaks().add(computeSavingStreak(user, allTx));
        response.getStreaks().add(computeBillPaymentStreak(user, allBudgetItems));
        response.setLoggingHeatmap(buildHeatmap(loggedDates, dailyLogCount, today));
        return response;
    }

    private StreakMetricDto computeLoggingStreak(Set<LocalDate> loggedDates, LocalDate today) {
        StreakResult result = countConsecutiveDays(
                today,
                date -> loggedDates.contains(date),
                true);

        boolean loggedToday = loggedDates.contains(today);
        boolean loggedYesterday = loggedDates.contains(today.minusDays(1));

        StreakMetricDto dto = new StreakMetricDto();
        dto.setType(StreakType.LOGGING);
        dto.setLabel("Logging streak");
        dto.setCurrent(result.current);
        dto.setBest(result.best);
        dto.setUnit("days");
        dto.setAtRisk(!loggedToday && loggedYesterday && result.current > 0);
        dto.setBroken(result.current == 0 && !loggedYesterday && result.best > 0);
        if (dto.isBroken()) {
            dto.setBrokenMessage("You missed logging yesterday — streak reset.");
        } else if (dto.isAtRisk()) {
            dto.setBrokenMessage("Log a transaction today to keep your streak alive.");
        }
        return dto;
    }

    private StreakMetricDto computeUnderBudgetStreak(
            User user, Map<LocalDate, BigDecimal> dailyExpense, LocalDate today) {
        StreakResult result = countConsecutiveDays(
                today.minusDays(1),
                date -> isUnderBudget(user, date, dailyExpense),
                false);

        boolean yesterdayUnder = isUnderBudget(user, today.minusDays(1), dailyExpense);
        boolean todayUnder = isUnderBudget(user, today, dailyExpense);

        StreakMetricDto dto = new StreakMetricDto();
        dto.setType(StreakType.UNDER_BUDGET);
        dto.setLabel("Under budget streak");
        dto.setCurrent(result.current);
        dto.setBest(result.best);
        dto.setUnit("days");
        dto.setAtRisk(!todayUnder && yesterdayUnder && result.current > 0);
        dto.setBroken(result.current == 0 && !yesterdayUnder && result.best > 0);
        if (dto.isBroken()) {
            dto.setBrokenMessage("You went over your daily budget yesterday — streak reset.");
        } else if (dto.isAtRisk()) {
            dto.setBrokenMessage("You're over budget today — log carefully to protect your streak.");
        }
        return dto;
    }

    private boolean isUnderBudget(User user, LocalDate date, Map<LocalDate, BigDecimal> dailyExpense) {
        BigDecimal dailyLimit = dailyBudgetFor(user, YearMonth.from(date));
        if (dailyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal spent = dailyExpense.getOrDefault(date, BigDecimal.ZERO);
        return spent.compareTo(dailyLimit) < 0;
    }

    private BigDecimal dailyBudgetFor(User user, YearMonth month) {
        Budget budget = budgetRepository.findByUserAndMonth(user, month).orElse(null);
        BigDecimal monthlyLimit = budget != null ? budget.getMonthlyLimit() : BigDecimal.ZERO;
        if (monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            monthlyLimit = categoryBudgetService.totalPlannedForMonth(user, month);
        }
        if (monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return monthlyLimit.divide(
                BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
    }

    private StreakMetricDto computeSavingStreak(User user, List<Transaction> allTx) {
        YearMonth current = YearMonth.now();
        YearMonth earliest = earliestDataMonth(allTx);
        if (earliest == null) {
            return emptyMonthlyStreak(StreakType.SAVING, "Saving streak", "No income data yet.");
        }

        List<Boolean> monthResults = new ArrayList<>();
        for (YearMonth m = earliest; !m.isAfter(current); m = m.plusMonths(1)) {
            monthResults.add(savingsRateAboveThreshold(user, m, allTx));
        }

        StreakResult result = countConsecutiveMonths(monthResults);
        boolean currentMonthOk = !monthResults.isEmpty()
                && monthResults.get(monthResults.size() - 1);
        boolean lastMonthOk = monthResults.size() >= 2
                && monthResults.get(monthResults.size() - 2);

        StreakMetricDto dto = new StreakMetricDto();
        dto.setType(StreakType.SAVING);
        dto.setLabel("Saving streak");
        dto.setCurrent(result.current);
        dto.setBest(result.best);
        dto.setUnit("months");
        dto.setAtRisk(!currentMonthOk && lastMonthOk && result.current > 0);
        dto.setBroken(result.current == 0 && !lastMonthOk && result.best > 0);
        if (dto.isBroken()) {
            dto.setBrokenMessage("Last month's savings rate dropped below 20% — streak ended.");
        } else if (dto.isAtRisk()) {
            dto.setBrokenMessage("Savings rate is below 20% this month — boost savings to keep the streak.");
        }
        return dto;
    }

    private boolean savingsRateAboveThreshold(User user, YearMonth month, List<Transaction> allTx) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        BigDecimal income = allTx.stream()
                .filter(t -> t.getDate() != null
                        && !t.getDate().isBefore(from)
                        && !t.getDate().isAfter(to)
                        && t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, month);
        double rate = income.subtract(expenses)
                .multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP)
                .doubleValue();
        return rate >= SAVINGS_RATE_THRESHOLD;
    }

    private StreakMetricDto computeBillPaymentStreak(User user, List<CategoryBudgetItem> allItems) {
        List<RecurringTransaction> recurring =
                recurringRepository.findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(user);
        boolean hasRecurringBills = recurring.stream()
                .anyMatch(r -> r.getType() == TransactionType.EXPENSE);

        YearMonth current = YearMonth.now();
        YearMonth earliest = earliestBillMonth(allItems, hasRecurringBills);
        if (earliest == null) {
            StreakMetricDto dto = emptyMonthlyStreak(
                    StreakType.BILL_PAYMENT, "Bill payment streak", null);
            dto.setBrokenMessage("Add recurring bills on Plan to start tracking.");
            return dto;
        }

        List<Boolean> monthResults = new ArrayList<>();
        for (YearMonth m = earliest; !m.isAfter(current); m = m.plusMonths(1)) {
            Boolean paid = allBillsPaidForMonth(user, m, allItems, recurring);
            if (paid != null) {
                monthResults.add(paid);
            }
        }

        if (monthResults.isEmpty()) {
            StreakMetricDto dto = emptyMonthlyStreak(
                    StreakType.BILL_PAYMENT, "Bill payment streak", null);
            dto.setBrokenMessage("Mark bills as paid on Plan to build your streak.");
            return dto;
        }

        StreakResult result = countConsecutiveMonths(monthResults);
        boolean currentOk = monthResults.get(monthResults.size() - 1);
        boolean lastOk = monthResults.size() >= 2 && monthResults.get(monthResults.size() - 2);

        StreakMetricDto dto = new StreakMetricDto();
        dto.setType(StreakType.BILL_PAYMENT);
        dto.setLabel("Bill payment streak");
        dto.setCurrent(result.current);
        dto.setBest(result.best);
        dto.setUnit("months");
        dto.setAtRisk(!currentOk && lastOk && result.current > 0);
        dto.setBroken(result.current == 0 && !lastOk && result.best > 0);
        if (dto.isBroken()) {
            dto.setBrokenMessage("Not all bills were marked paid last month — streak ended.");
        } else if (dto.isAtRisk()) {
            dto.setBrokenMessage("Some bills are still unpaid this month — mark them to keep your streak.");
        }
        return dto;
    }

    /**
     * @return true if all bills paid, false if any unpaid, null if no bills tracked that month
     */
    private Boolean allBillsPaidForMonth(
            User user,
            YearMonth month,
            List<CategoryBudgetItem> allItems,
            List<RecurringTransaction> recurring) {
        List<CategoryBudgetItem> bills = allItems.stream()
                .filter(i -> month.equals(i.getMonth()))
                .filter(i -> i.isFixed() || i.getDueDate() != null)
                .toList();

        if (!bills.isEmpty()) {
            return bills.stream().allMatch(CategoryBudgetItem::isPaid);
        }

        if (!hasRecurringExpenseInMonth(recurring, month)) {
            return null;
        }

        return recurring.stream()
                .filter(r -> r.getType() == TransactionType.EXPENSE)
                .allMatch(r -> recurringBillPaidForMonth(r, month, user));
    }

    private boolean hasRecurringExpenseInMonth(List<RecurringTransaction> recurring, YearMonth month) {
        return recurring.stream().anyMatch(r -> r.getType() == TransactionType.EXPENSE);
    }

    private boolean recurringBillPaidForMonth(
            RecurringTransaction recurring, YearMonth month, User user) {
        int day = Math.min(recurring.getDayOfMonth(), month.lengthOfMonth());
        LocalDate due = month.atDay(day);
        if (due.isAfter(LocalDate.now())) {
            return true;
        }
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        return transactionRepository.findByUserAndDateBetween(user, from, to).stream()
                .anyMatch(t -> t.getType() == TransactionType.EXPENSE
                        && recurring.getCategory().equalsIgnoreCase(t.getCategory())
                        && t.getAmount().compareTo(recurring.getAmount()) == 0);
    }

    private YearMonth earliestDataMonth(List<Transaction> allTx) {
        return allTx.stream()
                .map(Transaction::getDate)
                .filter(d -> d != null)
                .map(YearMonth::from)
                .min(YearMonth::compareTo)
                .orElse(null);
    }

    private YearMonth earliestBillMonth(List<CategoryBudgetItem> allItems, boolean hasRecurring) {
        YearMonth fromItems = allItems.stream()
                .map(CategoryBudgetItem::getMonth)
                .min(YearMonth::compareTo)
                .orElse(null);
        if (fromItems != null) {
            return fromItems;
        }
        return hasRecurring ? YearMonth.now().minusMonths(11) : null;
    }

    private List<LoggingDayDto> buildHeatmap(
            Set<LocalDate> loggedDates, Map<LocalDate, Integer> counts, LocalDate today) {
        List<LoggingDayDto> heatmap = new ArrayList<>();
        LocalDate start = today.minusDays(HEATMAP_DAYS - 1);
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            int count = counts.getOrDefault(d, 0);
            heatmap.add(new LoggingDayDto(d, loggedDates.contains(d), count));
        }
        return heatmap;
    }

    private StreakResult countConsecutiveDays(
            LocalDate endDate,
            java.util.function.Predicate<LocalDate> qualifies,
            boolean allowGraceToday) {
        LocalDate today = LocalDate.now();
        LocalDate start = endDate.minusDays(365);
        List<Boolean> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(endDate); d = d.plusDays(1)) {
            days.add(qualifies.test(d));
        }

        int current = 0;
        LocalDate cursor = endDate;
        if (allowGraceToday && !qualifies.test(today) && qualifies.test(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else if (allowGraceToday && qualifies.test(today)) {
            cursor = today;
        }

        for (LocalDate d = cursor; !d.isBefore(start); d = d.minusDays(1)) {
            if (qualifies.test(d)) {
                current++;
            } else {
                break;
            }
        }

        int best = 0;
        int run = 0;
        for (LocalDate d = start; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (qualifies.test(d)) {
                run++;
                best = Math.max(best, run);
            } else {
                run = 0;
            }
        }
        return new StreakResult(current, best);
    }

    private StreakResult countConsecutiveMonths(List<Boolean> monthResults) {
        if (monthResults.isEmpty()) {
            return new StreakResult(0, 0);
        }
        int current = 0;
        for (int i = monthResults.size() - 1; i >= 0; i--) {
            if (monthResults.get(i)) {
                current++;
            } else {
                break;
            }
        }
        int best = 0;
        int run = 0;
        for (boolean ok : monthResults) {
            if (ok) {
                run++;
                best = Math.max(best, run);
            } else {
                run = 0;
            }
        }
        return new StreakResult(current, best);
    }

    private StreakMetricDto emptyMonthlyStreak(StreakType type, String label, String message) {
        StreakMetricDto dto = new StreakMetricDto();
        dto.setType(type);
        dto.setLabel(label);
        dto.setCurrent(0);
        dto.setBest(0);
        dto.setUnit("months");
        dto.setBroken(false);
        if (message != null) {
            dto.setBrokenMessage(message);
        }
        return dto;
    }

    private record StreakResult(int current, int best) {}
}
