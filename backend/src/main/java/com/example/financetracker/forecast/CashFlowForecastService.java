package com.example.financetracker.forecast;

import com.example.financetracker.budget.CategoryBudgetItem;
import com.example.financetracker.budget.CategoryBudgetItemRepository;
import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.model.PlannedPurchase;
import com.example.financetracker.health.model.Subscription;
import com.example.financetracker.health.repo.FinancialProfileRepository;
import com.example.financetracker.health.repo.PlannedPurchaseRepository;
import com.example.financetracker.health.repo.SubscriptionRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CashFlowForecastService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter LONG_DAY =
            DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);

    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;
    private final CategoryBudgetItemRepository budgetItemRepository;
    private final CategoryBudgetService categoryBudgetService;
    private final SubscriptionRepository subscriptionRepository;
    private final FinancialProfileRepository profileRepository;
    private final PlannedPurchaseRepository plannedPurchaseRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public CashFlowForecastService(CurrentUserService currentUserService,
                                   TransactionRepository transactionRepository,
                                   CategoryBudgetItemRepository budgetItemRepository,
                                   CategoryBudgetService categoryBudgetService,
                                   SubscriptionRepository subscriptionRepository,
                                   FinancialProfileRepository profileRepository,
                                   PlannedPurchaseRepository plannedPurchaseRepository,
                                   RecurringTransactionRepository recurringTransactionRepository) {
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.categoryBudgetService = categoryBudgetService;
        this.subscriptionRepository = subscriptionRepository;
        this.profileRepository = profileRepository;
        this.plannedPurchaseRepository = plannedPurchaseRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
    }

    @Transactional(readOnly = true)
    public CashFlowForecastResponse forecast(YearMonth month) {
        User user = currentUserService.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        // Forecast remaining days of the selected month (from tomorrow if viewing current month)
        LocalDate from = monthStart;
        if (month.equals(YearMonth.from(today))) {
            from = today.plusDays(1);
            if (from.isAfter(monthEnd)) {
                return emptyForecast(monthBalanceSoFar(user, month), "No remaining days left in this month.");
            }
        } else if (month.isBefore(YearMonth.from(today))) {
            return emptyForecast(monthBalanceSoFar(user, month), "Selected month is in the past.");
        }

        BigDecimal startingBalance = resolveStartingBalance(user, month);
        List<RecurringEvent> events = collectRecurringEvents(user, month, from, monthEnd);
        Map<LocalDate, List<RecurringEvent>> byDay = new HashMap<>();
        for (RecurringEvent event : events) {
            byDay.computeIfAbsent(event.date(), d -> new ArrayList<>()).add(event);
        }

        BigDecimal running = startingBalance;
        List<CashFlowDayDto> days = new ArrayList<>();
        List<String> negativeDates = new ArrayList<>();
        LowestPointDto lowest = null;

        for (LocalDate day = from; !day.isAfter(monthEnd); day = day.plusDays(1)) {
            List<String> labels = new ArrayList<>();
            for (RecurringEvent event : byDay.getOrDefault(day, List.of())) {
                running = running.add(event.delta());
                labels.add(formatEventLabel(event));
            }
            BigDecimal balance = running.setScale(2, RoundingMode.HALF_UP);
            days.add(new CashFlowDayDto(day.format(ISO), balance, labels));

            if (lowest == null || balance.compareTo(lowest.getBalance()) < 0) {
                lowest = new LowestPointDto(day.format(ISO), balance);
            }
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                negativeDates.add(day.format(ISO));
            }
        }

        CashFlowForecastResponse response = new CashFlowForecastResponse();
        response.setStartingBalance(startingBalance.setScale(2, RoundingMode.HALF_UP));
        response.setDays(days);
        response.setLowestPoint(lowest);
        response.setWillGoNegative(!negativeDates.isEmpty());
        response.setNegativeDates(negativeDates);
        response.setSummary(buildSummary(response, byDay));
        return response;
    }

    private CashFlowForecastResponse emptyForecast(BigDecimal starting, String summary) {
        CashFlowForecastResponse response = new CashFlowForecastResponse();
        response.setStartingBalance(starting.setScale(2, RoundingMode.HALF_UP));
        response.setDays(List.of());
        response.setLowestPoint(new LowestPointDto(null, starting.setScale(2, RoundingMode.HALF_UP)));
        response.setWillGoNegative(starting.compareTo(BigDecimal.ZERO) < 0);
        response.setNegativeDates(List.of());
        response.setSummary(summary);
        return response;
    }

    /**
     * Prefer profile bank balance when configured (non-zero); otherwise month income − expenses so far.
     */
    private BigDecimal resolveStartingBalance(User user, YearMonth month) {
        BigDecimal mtd = monthBalanceSoFar(user, month);
        return profileRepository.findByUserId(user.getId())
                .map(FinancialProfile::getCurrentBalance)
                .filter(b -> b != null && b.compareTo(BigDecimal.ZERO) != 0)
                .orElse(mtd);
    }

    private BigDecimal monthBalanceSoFar(User user, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        LocalDate today = LocalDate.now();
        if (month.equals(YearMonth.from(today)) && today.isBefore(to)) {
            to = today;
        }
        List<Transaction> txs = transactionRepository.findByUserAndDateBetween(user, from, to);
        BigDecimal income = txs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, month);
        return income.subtract(expenses);
    }

    private List<RecurringEvent> collectRecurringEvents(User user,
                                                        YearMonth month,
                                                        LocalDate from,
                                                        LocalDate monthEnd) {
        List<RecurringEvent> events = new ArrayList<>();
        Set<String> coveredLabels = new HashSet<>();

        // 1) Unpaid budget lines with due dates (primary recurring bills)
        for (CategoryBudgetItem item : budgetItemRepository
                .findByUserAndMonthOrderByCategoryAscIdAsc(user, month)) {
            if (item.isPaid() || item.getDueDate() == null) {
                continue;
            }
            LocalDate due = item.getDueDate();
            if (due.isBefore(from) || due.isAfter(monthEnd)) {
                continue;
            }
            String label = item.getDescription() != null && !item.getDescription().isBlank()
                    ? item.getDescription()
                    : item.getCategory();
            BigDecimal amount = item.getPlannedAmount() != null
                    ? item.getPlannedAmount()
                    : BigDecimal.ZERO;
            events.add(new RecurringEvent(due, label, amount.negate()));
            coveredLabels.add(normalize(label));
            coveredLabels.add(normalize(item.getCategory()));
        }

        // 2) Salary credit on salary day
        FinancialProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null && profile.getSalaryDayOfMonth() != null) {
            int salaryDay = Math.min(Math.max(profile.getSalaryDayOfMonth(), 1), month.lengthOfMonth());
            LocalDate salaryDate = month.atDay(salaryDay);
            if (!salaryDate.isBefore(from) && !salaryDate.isAfter(monthEnd)) {
                BigDecimal salary = estimateMonthlyIncome(user, month);
                if (salary.compareTo(BigDecimal.ZERO) > 0) {
                    events.add(new RecurringEvent(salaryDate, "Salary", salary));
                }
            }
        }

        // 3) Active subscriptions (day 1 of month if still upcoming; skip if already covered by budget)
        LocalDate subDate = month.atDay(1);
        if (!subDate.isBefore(from) && !subDate.isAfter(monthEnd)) {
            for (Subscription sub : subscriptionRepository.findByUserIdAndActiveTrue(user.getId())) {
                if (coveredLabels.contains(normalize(sub.getName()))) {
                    continue;
                }
                BigDecimal amount = sub.getMonthlyAmount() != null
                        ? sub.getMonthlyAmount()
                        : BigDecimal.ZERO;
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                events.add(new RecurringEvent(subDate, sub.getName(), amount.negate()));
            }
        }

        // 4) User-defined recurring transactions (Plan page)
        for (RecurringTransaction item : recurringTransactionRepository
                .findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(user)) {
            int day = Math.min(Math.max(item.getDayOfMonth(), 1), month.lengthOfMonth());
            LocalDate date = month.atDay(day);
            if (date.isBefore(from) || date.isAfter(monthEnd)) {
                continue;
            }
            if (coveredLabels.contains(normalize(item.getName()))
                    || coveredLabels.contains(normalize(item.getCategory()))) {
                continue;
            }
            BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal delta = item.getType() == TransactionType.INCOME ? amount : amount.negate();
            events.add(new RecurringEvent(date, item.getName(), delta));
            coveredLabels.add(normalize(item.getName()));
        }

        // 5) Planned purchases still ahead
        for (PlannedPurchase purchase : plannedPurchaseRepository.findByUserIdAndActiveTrue(user.getId())) {
            LocalDate expected = purchase.getExpectedDate();
            if (expected == null || expected.isBefore(from) || expected.isAfter(monthEnd)) {
                continue;
            }
            BigDecimal amount = purchase.getAmount() != null ? purchase.getAmount() : BigDecimal.ZERO;
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            events.add(new RecurringEvent(expected, purchase.getName(), amount.negate()));
        }

        events.sort(Comparator.comparing(RecurringEvent::date));
        return events;
    }

    private BigDecimal estimateMonthlyIncome(User user, YearMonth month) {
        // Prefer prior full-month income as the expected salary credit
        YearMonth prior = month.minusMonths(1);
        BigDecimal priorIncome = transactionRepository
                .findByUserAndDateBetween(user, prior.atDay(1), prior.atEndOfMonth())
                .stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (priorIncome.compareTo(BigDecimal.ZERO) > 0) {
            return priorIncome;
        }
        LocalDate from = month.atDay(1);
        LocalDate to = LocalDate.now();
        if (to.isBefore(from)) {
            to = month.atEndOfMonth();
        }
        if (to.isAfter(month.atEndOfMonth())) {
            to = month.atEndOfMonth();
        }
        return transactionRepository.findByUserAndDateBetween(user, from, to).stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String formatEventLabel(RecurringEvent event) {
        String sign = event.delta().signum() >= 0 ? "+" : "-";
        BigDecimal abs = event.delta().abs().setScale(0, RoundingMode.HALF_UP);
        return event.label() + ": " + sign + "₹" + abs.toPlainString();
    }

    private String buildSummary(CashFlowForecastResponse response,
                                Map<LocalDate, List<RecurringEvent>> byDay) {
        LowestPointDto lowest = response.getLowestPoint();
        if (lowest == null || lowest.getDate() == null) {
            return response.getSummary() != null ? response.getSummary() : "No forecast available.";
        }
        LocalDate lowestDate = LocalDate.parse(lowest.getDate());
        String lowestLabel = lowestDate.format(LONG_DAY);
        String balanceLabel = "₹" + lowest.getBalance().setScale(0, RoundingMode.HALF_UP).toPlainString();

        if (!response.isWillGoNegative()) {
            return "Your balance stays positive all month. Lowest point: "
                    + balanceLabel + " on " + lowestLabel + ".";
        }

        String firstNeg = response.getNegativeDates().isEmpty()
                ? lowestLabel
                : LocalDate.parse(response.getNegativeDates().get(0)).format(LONG_DAY);
        List<RecurringEvent> triggers = byDay.getOrDefault(
                LocalDate.parse(response.getNegativeDates().get(0)), List.of());
        String cause = triggers.stream()
                .filter(e -> e.delta().signum() < 0)
                .map(RecurringEvent::label)
                .findFirst()
                .orElse("upcoming bills");
        return "Balance may go negative on " + firstNeg
                + " if " + cause + " is paid. Consider delaying an optional expense.";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
