package com.example.financetracker.transaction;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RegretService {

    private static final BigDecimal REGRET_THRESHOLD = new BigDecimal("1000");

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public RegretService(
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    static void applyRegretSchedule(Transaction tx) {
        if (tx.getType() == TransactionType.EXPENSE
                && tx.getAmount() != null
                && tx.getAmount().compareTo(REGRET_THRESHOLD) >= 0
                && tx.getDate() != null) {
            tx.setRegretStatus(RegretStatus.PENDING_REVIEW);
            tx.setRegretReviewDate(tx.getDate().plusDays(7));
        } else {
            tx.setRegretStatus(RegretStatus.NOT_APPLICABLE);
            tx.setRegretReviewDate(null);
        }
    }

    @Transactional(readOnly = true)
    public List<PendingRegretDto> pendingReviews() {
        User user = currentUserService.getCurrentUser();
        LocalDate today = LocalDate.now();
        return transactionRepository
                .findByUserAndRegretStatusAndRegretReviewDateLessThanEqualOrderByRegretReviewDateAsc(
                        user, RegretStatus.PENDING_REVIEW, today)
                .stream()
                .map(tx -> new PendingRegretDto(
                        tx.getId(),
                        tx.getDescription(),
                        tx.getCategory(),
                        tx.getAmount(),
                        tx.getDate(),
                        tx.getRegretReviewDate()))
                .toList();
    }

    @Transactional
    public Transaction updateRegret(Long id, RegretStatus status) {
        if (status != RegretStatus.REGRET
                && status != RegretStatus.NO_REGRET
                && status != RegretStatus.NEUTRAL) {
            throw new IllegalArgumentException("status must be REGRET, NO_REGRET, or NEUTRAL");
        }
        User user = currentUserService.getCurrentUser();
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (tx.getRegretStatus() != RegretStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Transaction is not pending regret review");
        }
        tx.setRegretStatus(status);
        tx.setRegretAskedAt(LocalDate.now());
        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public RegretStatsResponse stats() {
        User user = currentUserService.getCurrentUser();
        List<Transaction> reviewed = transactionRepository.findByUserAndRegretStatusIn(
                user, List.of(RegretStatus.REGRET, RegretStatus.NO_REGRET, RegretStatus.NEUTRAL));

        int totalRegret = 0;
        int totalNoRegret = 0;
        int totalNeutral = 0;
        double totalMoneyRegretted = 0;

        Map<String, int[]> categoryCounts = new HashMap<>();

        for (Transaction tx : reviewed) {
            String cat = tx.getCategory() != null ? tx.getCategory() : "Other";
            int[] counts = categoryCounts.computeIfAbsent(cat, k -> new int[2]);

            switch (tx.getRegretStatus()) {
                case REGRET -> {
                    totalRegret++;
                    counts[0]++;
                    counts[1]++;
                    totalMoneyRegretted += tx.getAmount().doubleValue();
                }
                case NO_REGRET -> {
                    totalNoRegret++;
                    counts[0]++;
                }
                case NEUTRAL -> {
                    totalNeutral++;
                    counts[0]++;
                }
                default -> {
                }
            }
        }

        int totalReviewed = totalRegret + totalNoRegret + totalNeutral;
        double regretRate = totalReviewed > 0
                ? round((totalRegret * 100.0) / totalReviewed)
                : 0;

        List<CategoryRegretStatsDto> byCategory = new ArrayList<>();
        String mostRegretted = null;
        double highestRegretRate = -1;
        String mostValued = null;
        double lowestRegretRate = 101;

        for (Map.Entry<String, int[]> e : categoryCounts.entrySet()) {
            int reviewedCount = e.getValue()[0];
            int regretCount = e.getValue()[1];
            double rate = reviewedCount > 0 ? (regretCount * 100.0) / reviewedCount : 0;
            byCategory.add(new CategoryRegretStatsDto(
                    e.getKey(), reviewedCount, regretCount, round(rate)));
            if (reviewedCount >= 1 && rate > highestRegretRate) {
                highestRegretRate = rate;
                mostRegretted = e.getKey();
            }
            if (reviewedCount >= 1 && rate < lowestRegretRate) {
                lowestRegretRate = rate;
                mostValued = e.getKey();
            }
        }

        byCategory.sort(Comparator.comparingDouble(CategoryRegretStatsDto::getRegretRate).reversed());

        RegretStatsResponse response = new RegretStatsResponse();
        response.setTotalReviewed(totalReviewed);
        response.setTotalRegret(totalRegret);
        response.setTotalNoRegret(totalNoRegret);
        response.setTotalNeutral(totalNeutral);
        response.setRegretRate(regretRate);
        response.setMostRegrettedCategory(mostRegretted != null ? mostRegretted : "—");
        response.setMostValuedCategory(mostValued != null ? mostValued : "—");
        response.setRegretByCategory(byCategory);
        response.setTotalMoneyRegretted(round(totalMoneyRegretted));
        response.setAverageRegrettedAmount(
                totalRegret > 0 ? round(totalMoneyRegretted / totalRegret) : 0);
        response.setRecentInsight(buildInsight(regretRate, mostRegretted, mostValued, byCategory));
        return response;
    }

    @Transactional(readOnly = true)
    public CategoryRegretStatsDto statsForCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        RegretStatsResponse all = stats();
        return all.getRegretByCategory().stream()
                .filter(c -> c.getCategory().equalsIgnoreCase(category.trim()))
                .findFirst()
                .orElse(null);
    }

    private static String buildInsight(
            double regretRate,
            String mostRegretted,
            String mostValued,
            List<CategoryRegretStatsDto> byCategory) {
        if (byCategory.isEmpty()) {
            return "Rate a few purchases to unlock personalized regret insights.";
        }

        CategoryRegretStatsDto top = byCategory.get(0);
        if (regretRate < 30) {
            return String.format(
                    Locale.ENGLISH,
                    "You make good purchase decisions — only %.0f%% of purchases are regretted. "
                            + "Your best category is %s.",
                    regretRate,
                    mostValued != null ? mostValued : "your top picks");
        }

        if (top.getRegretRate() > 50 && top.getReviewed() >= 2) {
            return String.format(
                    Locale.ENGLISH,
                    "You regret %.0f%% of %s purchases. Consider a 24-hour wait rule before "
                            + "buying in this category.",
                    top.getRegretRate(),
                    top.getCategory());
        }

        return String.format(
                Locale.ENGLISH,
                "You regret %.0f%% of purchases overall. Watch %s — it has your highest "
                        + "regret rate at %.0f%%.",
                regretRate,
                mostRegretted != null ? mostRegretted : "discretionary spending",
                top.getRegretRate());
    }

    public static long daysSincePurchase(LocalDate purchaseDate) {
        if (purchaseDate == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
