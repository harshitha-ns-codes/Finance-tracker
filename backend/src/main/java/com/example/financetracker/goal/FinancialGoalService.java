package com.example.financetracker.goal;

import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.repo.SavingsGoalRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinancialGoalService {

    private static final int[] MILESTONES = {25, 50, 75, 100};
    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final CurrentUserService currentUserService;
    private final CategoryBudgetService categoryBudgetService;
    private final TransactionRepository transactionRepository;

    public FinancialGoalService(
            SavingsGoalRepository goalRepository,
            GoalContributionRepository contributionRepository,
            CurrentUserService currentUserService,
            CategoryBudgetService categoryBudgetService,
            TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.currentUserService = currentUserService;
        this.categoryBudgetService = categoryBudgetService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<FinancialGoalDto> listGoals() {
        User user = currentUserService.getCurrentUser();
        syncLinkedCategories(user);
        BigDecimal monthlySavings = averageMonthlySavings(user);
        return goalRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                .map(g -> toDto(g, monthlySavings, List.of()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmergencyFundRecommendationDto emergencyRecommendation() {
        User user = currentUserService.getCurrentUser();
        BigDecimal avgExpenses = averageMonthlyExpenses(user);
        BigDecimal recommended = avgExpenses.multiply(BigDecimal.valueOf(6))
                .setScale(0, RoundingMode.HALF_UP);
        EmergencyFundRecommendationDto dto = new EmergencyFundRecommendationDto();
        dto.setAverageMonthlyExpenses(avgExpenses);
        dto.setRecommendedTarget(recommended);
        dto.setMessage(String.format(
                Locale.ENGLISH,
                "Your recommended emergency fund: %s based on %s average monthly expenses.",
                formatInr(recommended),
                formatInr(avgExpenses)));
        return dto;
    }

    @Transactional
    public FinancialGoalDto createGoal(FinancialGoalCreateRequest request) {
        User user = currentUserService.getCurrentUser();
        SavingsGoal goal = new SavingsGoal();
        goal.setUserId(user.getId());
        applyRequest(goal, request);
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setActive(true);
        goal = goalRepository.save(goal);
        BigDecimal initial = nz(request.getCurrentAmount());
        if (initial.compareTo(BigDecimal.ZERO) > 0) {
            addContribution(goal, initial, LocalDate.now(), "Initial balance", ContributionSource.MANUAL, null);
        }
        List<GoalMilestoneDto> newly = checkMilestones(goal);
        goal = goalRepository.save(goal);
        return toDto(goal, averageMonthlySavings(user), newly);
    }

    @Transactional
    public FinancialGoalDto contribute(Long id, GoalContributeRequest request) {
        User user = currentUserService.getCurrentUser();
        SavingsGoal goal = findGoal(user, id);
        BigDecimal amount = nz(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be positive");
        }
        addContribution(goal, amount, LocalDate.now(), request.getNote(), ContributionSource.MANUAL, null);
        List<GoalMilestoneDto> newly = checkMilestones(goal);
        goal = goalRepository.save(goal);
        return toDto(goal, averageMonthlySavings(user), newly);
    }

    @Transactional
    public void deleteGoal(Long id) {
        User user = currentUserService.getCurrentUser();
        SavingsGoal goal = findGoal(user, id);
        goal.setActive(false);
        goalRepository.save(goal);
    }

    @Transactional
    public void onTransactionCreated(Transaction tx) {
        if (tx.getUser() == null || tx.getCategory() == null || tx.getType() != TransactionType.INCOME) {
            return;
        }
        List<SavingsGoal> goals = goalRepository.findByUserIdAndLinkedCategoryAndActiveTrue(
                tx.getUser().getId(), tx.getCategory());
        for (SavingsGoal goal : goals) {
            if (contributionRepository.existsByGoalIdAndTransactionId(goal.getId(), tx.getId())) {
                continue;
            }
            addContribution(goal, tx.getAmount(), tx.getDate(), "Auto from transaction", ContributionSource.AUTO, tx.getId());
            checkMilestones(goal);
            goalRepository.save(goal);
        }
    }

    private void syncLinkedCategories(User user) {
        List<SavingsGoal> linked = goalRepository.findByUserIdAndLinkedCategoryIsNotNullAndActiveTrue(user.getId());
        for (SavingsGoal goal : linked) {
            if (goal.getLinkedCategory() == null || goal.getLinkedCategory().isBlank()) {
                continue;
            }
            LocalDate from = goal.getCreatedAt() != null
                    ? goal.getCreatedAt().toLocalDate()
                    : LocalDate.now().minusYears(1);
            List<Transaction> txs = transactionRepository.findByUserAndDateBetween(user, from, LocalDate.now());
            for (Transaction tx : txs) {
                if (tx.getType() != TransactionType.INCOME
                        || !goal.getLinkedCategory().equalsIgnoreCase(tx.getCategory())) {
                    continue;
                }
                if (!contributionRepository.existsByGoalIdAndTransactionId(goal.getId(), tx.getId())) {
                    addContribution(goal, tx.getAmount(), tx.getDate(), "Auto from transaction",
                            ContributionSource.AUTO, tx.getId());
                }
            }
            checkMilestones(goal);
            goalRepository.save(goal);
        }
    }

    private void addContribution(
            SavingsGoal goal,
            BigDecimal amount,
            LocalDate date,
            String note,
            ContributionSource source,
            Long transactionId) {
        GoalContribution c = new GoalContribution();
        c.setGoalId(goal.getId());
        c.setAmount(amount);
        c.setDate(date != null ? date : LocalDate.now());
        c.setNote(note);
        c.setSource(source);
        c.setTransactionId(transactionId);
        contributionRepository.save(c);
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
    }

    private List<GoalMilestoneDto> checkMilestones(SavingsGoal goal) {
        double progress = progressPercent(goal);
        Set<Integer> reached = parseMilestones(goal.getMilestonesReached());
        List<GoalMilestoneDto> newly = new ArrayList<>();
        for (int m : MILESTONES) {
            if (progress >= m && !reached.contains(m)) {
                reached.add(m);
                newly.add(new GoalMilestoneDto(
                        m,
                        true,
                        String.format("You've hit %d%% of your %s! 🎉", m, goal.getName())));
            }
        }
        goal.setMilestonesReached(reached.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        return newly;
    }

    private FinancialGoalDto toDto(SavingsGoal goal, BigDecimal monthlySavings, List<GoalMilestoneDto> newly) {
        double progress = progressPercent(goal);
        Set<Integer> reached = parseMilestones(goal.getMilestonesReached());

        FinancialGoalDto dto = new FinancialGoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setGoalType(goal.getGoalType() != null ? goal.getGoalType() : GoalType.CUSTOM);
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setProgressPercent(progress);
        dto.setTargetDate(goal.getDeadline());
        dto.setLinkedCategory(goal.getLinkedCategory());
        dto.setNewlyAchievedMilestones(newly);

        List<GoalMilestoneDto> milestones = new ArrayList<>();
        for (int m : MILESTONES) {
            milestones.add(new GoalMilestoneDto(m, reached.contains(m), null));
        }
        dto.setMilestones(milestones);

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
        if (monthlySavings.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(BigDecimal.ZERO) > 0) {
            long months = remaining.divide(monthlySavings, 0, RoundingMode.CEILING).longValue();
            dto.setProjectedCompletionDate(LocalDate.now().plusMonths(months));
            dto.setInsight(String.format(
                    Locale.ENGLISH,
                    "At %s/month savings rate, you'll reach %s by %s.",
                    formatInr(monthlySavings),
                    formatInr(goal.getTargetAmount()),
                    dto.getProjectedCompletionDate().format(MONTH_YEAR)));
        } else if (progress >= 100) {
            dto.setInsight("Goal complete — great work!");
        } else {
            dto.setInsight("Add contributions or link a savings category to track progress automatically.");
        }

        if (goal.getDeadline() != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            long monthsLeft = Math.max(1, ChronoUnit.MONTHS.between(
                    YearMonth.from(LocalDate.now()), YearMonth.from(goal.getDeadline())) + 1);
            BigDecimal needed = remaining.divide(BigDecimal.valueOf(monthsLeft), 0, RoundingMode.CEILING);
            dto.setMonthlyContributionNeeded(needed);

            if (monthlySavings.compareTo(BigDecimal.ZERO) > 0 && dto.getProjectedCompletionDate() != null) {
                long projectedMonths = ChronoUnit.MONTHS.between(
                        YearMonth.from(LocalDate.now()), YearMonth.from(dto.getProjectedCompletionDate()));
                double aheadBehind = monthsLeft - projectedMonths;
                dto.setMonthsAheadBehind(aheadBehind);
                if (aheadBehind > 0.5) {
                    dto.setScheduleMessage(String.format(
                            Locale.ENGLISH,
                            "You're %.0f months ahead of schedule.",
                            aheadBehind));
                } else if (aheadBehind < -0.5) {
                    dto.setScheduleMessage(String.format(
                            Locale.ENGLISH,
                            "You're %.0f months behind schedule.",
                            Math.abs(aheadBehind)));
                } else {
                    dto.setScheduleMessage("You're on track to hit your target date.");
                }
            }
        }

        return dto;
    }

    private void applyRequest(SavingsGoal goal, FinancialGoalCreateRequest request) {
        goal.setName(request.getName().trim());
        goal.setGoalType(request.getGoalType() != null ? request.getGoalType() : GoalType.CUSTOM);
        goal.setTargetAmount(nz(request.getTargetAmount()));
        goal.setDeadline(request.getTargetDate());
        goal.setLinkedCategory(request.getLinkedCategory() != null && !request.getLinkedCategory().isBlank()
                ? request.getLinkedCategory().trim()
                : null);
    }

    private SavingsGoal findGoal(User user, Long id) {
        return goalRepository.findById(id)
                .filter(g -> g.getUserId().equals(user.getId()) && g.isActive())
                .orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    private BigDecimal averageMonthlyExpenses(User user) {
        BigDecimal total = BigDecimal.ZERO;
        int months = 0;
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 3; i++) {
            YearMonth m = now.minusMonths(i);
            BigDecimal spent = categoryBudgetService.totalEffectiveSpent(user, m);
            if (spent.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(spent);
                months++;
            }
        }
        if (months == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMonthlySavings(User user) {
        BigDecimal total = BigDecimal.ZERO;
        int months = 0;
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 3; i++) {
            YearMonth m = now.minusMonths(i);
            LocalDate from = m.atDay(1);
            LocalDate to = m.atEndOfMonth();
            BigDecimal income = transactionRepository.findByUserAndDateBetween(user, from, to).stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, m);
            BigDecimal savings = income.subtract(expenses);
            if (savings.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(savings);
                months++;
            }
        }
        if (months == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
    }

    private static double progressPercent(SavingsGoal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return goal.getCurrentAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static Set<Integer> parseMilestones(String raw) {
        if (raw == null || raw.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String formatInr(BigDecimal amount) {
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
