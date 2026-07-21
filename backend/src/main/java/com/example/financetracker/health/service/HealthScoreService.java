package com.example.financetracker.health.service;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.advisory.service.FinancialAdvisoryService;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.dto.HealthScoreResponse;
import com.example.financetracker.health.dto.PurchaseDecisionResponse;
import com.example.financetracker.health.dto.PurchaseEvaluateRequest;
import com.example.financetracker.health.insight.InsightGenerator;
import com.example.financetracker.health.model.HealthScoreSnapshot;
import com.example.financetracker.health.recommendation.RecommendationGenerator;
import com.example.financetracker.health.repo.HealthScoreSnapshotRepository;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
public class HealthScoreService {

    private final CurrentUserService currentUserService;
    private final HealthScoreContextFactory contextFactory;
    private final HealthScoreAggregator aggregator;
    private final InsightGenerator insightGenerator;
    private final RecommendationGenerator recommendationGenerator;
    private final FinancialAdvisoryService financialAdvisoryService;
    private final HealthScoreSnapshotRepository snapshotRepository;

    public HealthScoreService(
            CurrentUserService currentUserService,
            HealthScoreContextFactory contextFactory,
            HealthScoreAggregator aggregator,
            InsightGenerator insightGenerator,
            RecommendationGenerator recommendationGenerator,
            FinancialAdvisoryService financialAdvisoryService,
            HealthScoreSnapshotRepository snapshotRepository) {
        this.currentUserService = currentUserService;
        this.contextFactory = contextFactory;
        this.aggregator = aggregator;
        this.insightGenerator = insightGenerator;
        this.recommendationGenerator = recommendationGenerator;
        this.financialAdvisoryService = financialAdvisoryService;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public HealthScoreResponse getScore(String monthStr) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(monthStr);
        HealthScoreContext ctx = contextFactory.build(user, month);
        List<CategoryScoreDto> breakdown = aggregator.calculateAll(ctx);
        int score = aggregator.totalScore(breakdown);

        HealthScoreResponse response = new HealthScoreResponse();
        response.setScore(score);
        response.setRating(aggregator.rating(score));
        response.setBreakdown(breakdown);
        response.setPositives(insightGenerator.positives(breakdown));
        response.setNegatives(insightGenerator.negatives(breakdown));
        response.setRecommendations(recommendationGenerator.recommendations(ctx, breakdown));

        if (ctx.getPreviousScore() != null) {
            response.setMonthDelta(score - ctx.getPreviousScore());
        }

        upsertSnapshot(user.getId(), month.toString(), score);
        return response;
    }

    @Transactional(readOnly = true)
    public PurchaseDecisionResponse evaluatePurchase(PurchaseEvaluateRequest request) {
        return financialAdvisoryService.evaluatePurchaseLegacy(request);
    }

    private void upsertSnapshot(Long userId, String month, int score) {
        HealthScoreSnapshot snap = snapshotRepository.findByUserIdAndScoreMonth(userId, month)
                .orElseGet(HealthScoreSnapshot::new);
        snap.setUserId(userId);
        snap.setScoreMonth(month);
        snap.setScore(score);
        snapshotRepository.save(snap);
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }
    }
}
