package com.example.financetracker.analyzer;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.transaction.NeedType;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.transaction.TransactionRepository;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Analyzer502030Service {

    private static final double NEEDS_IDEAL = 50;
    private static final double WANTS_IDEAL = 30;
    private static final double SAVINGS_IDEAL = 20;

    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;

    public Analyzer502030Service(
            CurrentUserService currentUserService,
            TransactionRepository transactionRepository) {
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Rule502030Response analyze(String monthStr) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = YearMonth.parse(monthStr);

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(
                user, month.atDay(1), month.atEndOfMonth());

        double totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

        Map<String, Double> needCategories = new HashMap<>();
        Map<String, Double> wantCategories = new HashMap<>();
        double totalNeeds = 0;
        double totalWants = 0;
        double unclassifiedAmount = 0;
        List<UnclassifiedTransactionDto> unclassified = new ArrayList<>();

        for (Transaction tx : transactions) {
            if (tx.getType() != TransactionType.EXPENSE) {
                continue;
            }
            double amount = tx.getAmount().doubleValue();
            NeedType effective = NeedTypeClassifier.effectiveType(tx.getNeedType(), tx.getCategory());

            if (NeedTypeClassifier.needsUserClassification(tx.getNeedType(), tx.getCategory())) {
                unclassifiedAmount += amount;
                unclassified.add(new UnclassifiedTransactionDto(
                        tx.getId(),
                        tx.getDescription(),
                        tx.getCategory(),
                        round(amount)));
            }

            switch (effective) {
                case NEED -> {
                    totalNeeds += amount;
                    needCategories.merge(tx.getCategory(), amount, Double::sum);
                }
                case SAVING -> {
                    // Intentional savings (SIP, investments) — not needs or wants
                }
                case WANT -> {
                    totalWants += amount;
                    wantCategories.merge(tx.getCategory(), amount, Double::sum);
                }
                default -> {
                    totalWants += amount;
                    wantCategories.merge(tx.getCategory(), amount, Double::sum);
                }
            }
        }

        double totalSaving = totalIncome - totalNeeds - totalWants;

        double needsPercent = pct(totalNeeds, totalIncome);
        double wantsPercent = pct(totalWants, totalIncome);
        double savingsPercent = pct(totalSaving, totalIncome);

        String topWantCategory = topCategory(wantCategories);

        BucketAnalysisDto needs = buildBucket(
                totalNeeds, needsPercent, NEEDS_IDEAL, needCategories, topWantCategory, true);
        BucketAnalysisDto wants = buildBucket(
                totalWants, wantsPercent, WANTS_IDEAL, wantCategories, topWantCategory, false);
        BucketAnalysisDto savings = buildSavingsBucket(
                totalSaving, savingsPercent, topWantCategory);

        Rule502030Response response = new Rule502030Response();
        response.setMonth(monthStr);
        response.setTotalIncome(round(totalIncome));
        response.setNeeds(needs);
        response.setWants(wants);
        response.setSavings(savings);
        response.setUnclassifiedAmount(round(unclassifiedAmount));
        response.setUnclassifiedTransactions(unclassified);
        applyOverall(response);
        return response;
    }

    private BucketAnalysisDto buildBucket(
            double amount,
            double percent,
            double ideal,
            Map<String, Double> categoryTotals,
            String topWantCategory,
            boolean isNeeds) {
        double diff = round(percent - ideal);
        String status = statusFor(diff);
        String insight = isNeeds
                ? needsInsight(status, percent, topWantCategory)
                : wantsInsight(status, percent, topWantCategory);

        BucketAnalysisDto bucket = new BucketAnalysisDto();
        bucket.setAmount(round(amount));
        bucket.setPercent(round(percent));
        bucket.setIdealPercent(ideal);
        bucket.setDiff(diff);
        bucket.setStatus(status);
        bucket.setInsight(insight);
        bucket.setTopCategories(topCategories(categoryTotals, 3));
        return bucket;
    }

    private BucketAnalysisDto buildSavingsBucket(
            double amount, double percent, String topWantCategory) {
        double diff = round(percent - SAVINGS_IDEAL);
        String status = statusFor(diff);
        String insight = savingsInsight(status, percent, topWantCategory);

        BucketAnalysisDto bucket = new BucketAnalysisDto();
        bucket.setAmount(round(amount));
        bucket.setPercent(round(percent));
        bucket.setIdealPercent(SAVINGS_IDEAL);
        bucket.setDiff(diff);
        bucket.setStatus(status);
        bucket.setInsight(insight);
        return bucket;
    }

    private static String statusFor(double diff) {
        if (Math.abs(diff) <= 5) {
            return "ON_TRACK";
        }
        return diff > 0 ? "OVER" : "UNDER";
    }

    private static String needsInsight(String status, double percent, String topWant) {
        return switch (status) {
            case "OVER" -> String.format(
                    "Your needs are eating %.0f%% of income vs the ideal 50%%. "
                            + "Look for ways to reduce fixed costs.",
                    percent);
            case "UNDER" -> String.format(
                    "Your needs are well under control at %.0f%% — great efficiency.",
                    percent);
            default -> String.format(
                    "Your needs are at %.0f%%, close to the 50%% guideline.", percent);
        };
    }

    private static String wantsInsight(String status, double percent, String topWant) {
        String top = topWant != null ? topWant : "discretionary";
        return switch (status) {
            case "OVER" -> String.format(
                    "Discretionary spending at %.0f%% is above the ideal 30%%. "
                            + "Consider cutting back on %s.",
                    percent, top);
            case "UNDER" -> String.format(
                    "You're keeping wants at %.0f%% — well within the 30%% guideline.",
                    percent);
            default -> String.format(
                    "Your wants are at %.0f%%, near the 30%% target.", percent);
        };
    }

    private static String savingsInsight(String status, double percent, String topWant) {
        String top = topWant != null ? topWant : "discretionary";
        return switch (status) {
            case "OVER" -> String.format(
                    "Excellent — you're saving %.0f%% of income, beating the 20%% benchmark.",
                    percent);
            case "UNDER" -> String.format(
                    "Your savings rate of %.0f%% is below the 20%% target. "
                            + "Try to cut %s spending.",
                    percent, top);
            default -> String.format(
                    "Your savings rate is %.0f%%, near the 20%% target.", percent);
        };
    }

    private static List<CategoryAmountDto> topCategories(Map<String, Double> totals, int limit) {
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(e -> new CategoryAmountDto(e.getKey(), round(e.getValue())))
                .toList();
    }

    private static String topCategory(Map<String, Double> totals) {
        return totals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void applyOverall(Rule502030Response response) {
        int overCount = 0;
        if ("OVER".equals(response.getNeeds().getStatus())) overCount++;
        if ("OVER".equals(response.getWants().getStatus())) overCount++;
        if ("UNDER".equals(response.getSavings().getStatus())) overCount++;

        String focus = response.getWants().getTopCategories().isEmpty()
                ? "wants"
                : response.getWants().getTopCategories().get(0).getCategory();

        if (overCount >= 2 || ("OVER".equals(response.getNeeds().getStatus())
                && "UNDER".equals(response.getSavings().getStatus()))) {
            response.setOverallStatus("CRITICAL");
            response.setOverallInsight(
                    "Your spending pattern needs significant rebalancing. Focus on reducing "
                            + focus + ".");
        } else if (overCount >= 1) {
            response.setOverallStatus("NEEDS_ATTENTION");
            response.setOverallInsight(
                    "One or more categories need adjustment. See insights above.");
        } else {
            response.setOverallStatus("HEALTHY");
            response.setOverallInsight(
                    "Your spending follows the 50/30/20 rule closely. Great financial discipline.");
        }
    }

    private static double pct(double part, double whole) {
        if (whole <= 0) {
            return 0;
        }
        return part * 100.0 / whole;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
