package com.example.financetracker.health.advisory.service;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.FinancialAdvisoryContextFactory;
import com.example.financetracker.health.advisory.dto.*;
import com.example.financetracker.health.dto.PurchaseDecisionResponse;
import com.example.financetracker.health.dto.PurchaseEvaluateRequest;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
public class FinancialAdvisoryService {

    private final CurrentUserService currentUserService;
    private final FinancialAdvisoryContextFactory contextFactory;
    private final PredictionService predictionService;
    private final RecommendationService recommendationService;
    private final PurchaseDecisionService purchaseDecisionService;
    private final SimulationService simulationService;

    public FinancialAdvisoryService(
            CurrentUserService currentUserService,
            FinancialAdvisoryContextFactory contextFactory,
            PredictionService predictionService,
            RecommendationService recommendationService,
            PurchaseDecisionService purchaseDecisionService,
            SimulationService simulationService) {
        this.currentUserService = currentUserService;
        this.contextFactory = contextFactory;
        this.predictionService = predictionService;
        this.recommendationService = recommendationService;
        this.purchaseDecisionService = purchaseDecisionService;
        this.simulationService = simulationService;
    }

    @Transactional(readOnly = true)
    public FinancialAdvisoryResponse getAdvisory(String monthStr) {
        FinancialAdvisoryContext ctx = buildContext(monthStr);
        return buildResponse(ctx, null, List.of());
    }

    @Transactional(readOnly = true)
    public FinancialAdvisoryResponse evaluatePurchase(PurchaseEvaluateDetailRequest request) {
        FinancialAdvisoryContext ctx = buildContext(null);
        PurchaseDecisionDetailDto decision = purchaseDecisionService.evaluate(ctx, request);
        return buildResponse(ctx, decision, List.of());
    }

    @Transactional(readOnly = true)
    public FinancialAdvisoryResponse simulate(String monthStr, List<SimulationRequest> scenarios) {
        FinancialAdvisoryContext ctx = buildContext(monthStr);
        List<SimulationResultDto> results = simulationService.runScenarios(ctx, scenarios);
        return buildResponse(ctx, null, results);
    }

    /** Backward-compatible wrapper for legacy health-score purchase endpoint. */
    @Transactional(readOnly = true)
    public PurchaseDecisionResponse evaluatePurchaseLegacy(PurchaseEvaluateRequest request) {
        PurchaseEvaluateDetailRequest detail = new PurchaseEvaluateDetailRequest();
        detail.setPrice(request.getPrice());
        detail.setLabel(request.getLabel());
        detail.setPriority("WANT");
        PurchaseDecisionDetailDto full = purchaseDecisionService.evaluate(buildContext(null), detail);

        PurchaseDecisionResponse legacy = new PurchaseDecisionResponse();
        legacy.setDecision(mapLegacyDecision(full.getDecision()));
        legacy.setConfidence(full.getConfidence());
        legacy.setReason(full.getExplanation());
        legacy.setAlternatives(full.getAlternatives());
        return legacy;
    }

    private FinancialAdvisoryResponse buildResponse(
            FinancialAdvisoryContext ctx,
            PurchaseDecisionDetailDto decision,
            List<SimulationResultDto> simulations) {

        var prediction = predictionService.predict(ctx);
        FinancialAdvisoryResponse response = new FinancialAdvisoryResponse();
        response.setPrediction(prediction);
        response.setPurchaseDecision(decision);
        response.setRecommendations(recommendationService.generate(ctx, prediction));
        response.setSimulations(simulations);
        return response;
    }

    private FinancialAdvisoryContext buildContext(String monthStr) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(monthStr);
        return contextFactory.build(user, month);
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return YearMonth.now();
        return YearMonth.parse(month);
    }

    private static String mapLegacyDecision(String decision) {
        if ("BUY WITH CAUTION".equals(decision)) return "BUY";
        return decision;
    }
}
