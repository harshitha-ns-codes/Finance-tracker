package com.example.financetracker.simulate;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.advisory.AdvisoryMath;
import com.example.financetracker.health.advisory.FinancialAdvisoryContext;
import com.example.financetracker.health.advisory.FinancialAdvisoryContextFactory;
import com.example.financetracker.health.advisory.dto.ImpactAnalysisDto;
import com.example.financetracker.health.advisory.dto.PurchaseDecisionDetailDto;
import com.example.financetracker.health.advisory.dto.PurchaseEvaluateDetailRequest;
import com.example.financetracker.health.advisory.service.PurchaseDecisionService;
import com.example.financetracker.health.advisory.simulation.FinancialSimulator;
import com.example.financetracker.health.calculator.HealthScoreAggregator;
import com.example.financetracker.health.calculator.HealthScoreContext;
import com.example.financetracker.health.dto.CategoryScoreDto;
import com.example.financetracker.health.model.SavingsGoal;
import com.example.financetracker.health.service.HealthScoreContextFactory;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PurchaseSimulationService {

    private final CurrentUserService currentUserService;
    private final FinancialAdvisoryContextFactory advisoryContextFactory;
    private final HealthScoreContextFactory healthContextFactory;
    private final HealthScoreAggregator aggregator;
    private final FinancialSimulator financialSimulator;
    private final PurchaseDecisionService purchaseDecisionService;

    public PurchaseSimulationService(
            CurrentUserService currentUserService,
            FinancialAdvisoryContextFactory advisoryContextFactory,
            HealthScoreContextFactory healthContextFactory,
            HealthScoreAggregator aggregator,
            FinancialSimulator financialSimulator,
            PurchaseDecisionService purchaseDecisionService) {
        this.currentUserService = currentUserService;
        this.advisoryContextFactory = advisoryContextFactory;
        this.healthContextFactory = healthContextFactory;
        this.aggregator = aggregator;
        this.financialSimulator = financialSimulator;
        this.purchaseDecisionService = purchaseDecisionService;
    }

    @Transactional(readOnly = true)
    public PurchaseSimulationResponse simulate(PurchaseSimulationRequest request) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = YearMonth.now();
        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();

        HealthScoreContext health = healthContextFactory.build(user, month);
        FinancialAdvisoryContext advisoryCtx = advisoryContextFactory.build(user, month, paymentDate);

        BigDecimal price = request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO;
        String category = request.getCategory() != null ? request.getCategory() : "Miscellaneous";
        String priority = normalizePriority(request.getPriority());
        String itemName = request.getItemName() != null && !request.getItemName().isBlank()
                ? request.getItemName()
                : "this purchase";

        List<CategoryScoreDto> breakdown = aggregator.calculateAll(health);
        int currentScore = aggregator.totalScore(breakdown);

        BigDecimal balanceBefore = AdvisoryMath.spendableBalance(
                advisoryCtx.getCurrentBalance(), health);
        ImpactAnalysisDto impact = financialSimulator.simulatePurchase(advisoryCtx, price, category);
        BigDecimal balanceAfter = impact.getBalanceAfterPurchase() != null
                ? impact.getBalanceAfterPurchase()
                : balanceBefore.subtract(price);

        PurchaseEvaluateDetailRequest evalReq = new PurchaseEvaluateDetailRequest();
        evalReq.setPrice(price);
        evalReq.setLabel(itemName);
        evalReq.setCategory(category);
        evalReq.setPriority(mapPriorityForEngine(priority));
        PurchaseDecisionDetailDto decision = purchaseDecisionService.evaluate(advisoryCtx, evalReq);
        int affordabilityScore = decision.getAffordabilityScore();

        // --- Immediate balance ---
        PurchaseSimulationResponse.ImmediateImpact immediate =
                new PurchaseSimulationResponse.ImmediateImpact();
        immediate.setNewBalance(money(balanceAfter));
        immediate.setBalanceChange(money(price.negate()));
        double pctOfBalance = balanceBefore.signum() > 0
                ? price.multiply(BigDecimal.valueOf(100))
                .divide(balanceBefore, 1, RoundingMode.HALF_UP)
                .doubleValue()
                : (price.signum() > 0 ? 100.0 : 0.0);
        immediate.setPercentOfBalance(pctOfBalance);

        // --- Category budget ---
        BigDecimal categoryBudget = advisoryCtx.getCategoryBudgets()
                .getOrDefault(category, BigDecimal.ZERO);
        BigDecimal categorySpent = health.getSpentByCategory()
                .getOrDefault(category, BigDecimal.ZERO);
        BigDecimal afterPurchase = categorySpent.add(price);
        boolean willExceed = categoryBudget.signum() > 0
                && afterPurchase.compareTo(categoryBudget) > 0;
        BigDecimal exceedBy = willExceed
                ? afterPurchase.subtract(categoryBudget)
                : BigDecimal.ZERO;

        PurchaseSimulationResponse.BudgetImpact budget =
                new PurchaseSimulationResponse.BudgetImpact();
        budget.setCategoryBudget(money(categoryBudget));
        budget.setCategorySpent(money(categorySpent));
        budget.setAfterPurchase(money(afterPurchase));
        budget.setWillExceedBudget(willExceed);
        budget.setExceedBy(money(exceedBy));

        // --- Savings ---
        BigDecimal currentSavings = health.monthSavings();
        BigDecimal afterSavings = currentSavings.subtract(price);
        double rateBefore = savingsRate(health.getMonthIncome(), currentSavings);
        double rateAfter = savingsRate(health.getMonthIncome(), afterSavings);

        PurchaseSimulationResponse.SavingsImpact savings =
                new PurchaseSimulationResponse.SavingsImpact();
        savings.setCurrentMonthlySavings(money(currentSavings));
        savings.setAfterPurchaseSavings(money(afterSavings));
        savings.setCurrentSavingsRate(rateBefore);
        savings.setAfterSavingsRate(rateAfter);
        savings.setSavingsRateChange(round1(rateAfter - rateBefore));

        // --- Health score projection ---
        int projectedScore = projectHealthScore(
                currentScore, affordabilityScore, willExceed, balanceAfter, rateAfter - rateBefore, priority);
        int change = projectedScore - currentScore;
        String healthReason = healthReason(change, willExceed, balanceAfter, category);

        PurchaseSimulationResponse.HealthScoreImpact healthImpact =
                new PurchaseSimulationResponse.HealthScoreImpact();
        healthImpact.setCurrentScore(currentScore);
        healthImpact.setProjectedScore(projectedScore);
        healthImpact.setChange(change);
        healthImpact.setReason(healthReason);

        // --- Verdict ---
        String verdict = mapVerdict(affordabilityScore, willExceed, balanceAfter, priority);
        boolean canAfford = !"AVOID".equals(verdict) && balanceAfter.compareTo(BigDecimal.ZERO) >= 0;

        PurchaseSimulationResponse response = new PurchaseSimulationResponse();
        response.setCanAfford(canAfford);
        response.setAffordabilityScore(affordabilityScore);
        response.setImmediateImpact(immediate);
        response.setBudgetImpact(budget);
        response.setHealthScoreImpact(healthImpact);
        response.setSavingsImpact(savings);
        response.setVerdict(verdict);
        response.setVerdictReason(buildVerdictReason(
                itemName, price, verdict, willExceed, exceedBy, category, balanceAfter, affordabilityScore));
        response.setAlternatives(buildAlternatives(
                itemName, price, category, willExceed, exceedBy, impact, health, paymentDate, decision));
        return response;
    }

    private static String normalizePriority(String raw) {
        if (raw == null || raw.isBlank()) return "WANT";
        String p = raw.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "NEED", "NECESSITY" -> "NEED";
            case "INVESTMENT", "INVEST" -> "INVESTMENT";
            case "LUXURY" -> "WANT";
            default -> "WANT";
        };
    }

    private static String mapPriorityForEngine(String priority) {
        return switch (priority) {
            case "NEED" -> "NECESSITY";
            case "INVESTMENT" -> "WANT";
            default -> "WANT";
        };
    }

    private static String mapVerdict(
            int affordabilityScore,
            boolean willExceed,
            BigDecimal balanceAfter,
            String priority) {
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0 && !"NEED".equals(priority)) {
            return "AVOID";
        }
        if (affordabilityScore >= 85 && !willExceed) {
            return "GO_AHEAD";
        }
        if (affordabilityScore >= 70 && (!willExceed || "NEED".equals(priority))) {
            return "GO_AHEAD";
        }
        if (affordabilityScore >= 50 || "NEED".equals(priority) || "INVESTMENT".equals(priority)) {
            return "CONSIDER";
        }
        if (willExceed && affordabilityScore < 50) {
            return "AVOID";
        }
        return affordabilityScore >= 40 ? "CONSIDER" : "AVOID";
    }

    private static int projectHealthScore(
            int current,
            int affordability,
            boolean willExceed,
            BigDecimal balanceAfter,
            double savingsRateDelta,
            String priority) {
        int delta = 0;
        if (willExceed) delta -= 8;
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) delta -= 10;
        if (savingsRateDelta <= -15) delta -= 6;
        else if (savingsRateDelta <= -5) delta -= 3;
        if (affordability < 50) delta -= 5;
        else if (affordability >= 85) delta += 1;
        if ("NEED".equals(priority)) delta += 2;
        if ("INVESTMENT".equals(priority)) delta += 1;
        return Math.max(0, Math.min(100, current + delta));
    }

    private static String healthReason(
            int change, boolean willExceed, BigDecimal balanceAfter, String category) {
        if (change >= 0) {
            return "This purchase fits your current plan with little pressure on your score.";
        }
        List<String> parts = new ArrayList<>();
        if (willExceed) {
            parts.add(category + " budget would be exceeded");
        }
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            parts.add("balance would go negative");
        }
        if (parts.isEmpty()) {
            parts.add("lower savings cushion and discretionary spend");
        }
        return "Score dips mainly because " + String.join(" and ", parts) + ".";
    }

    private static String buildVerdictReason(
            String itemName,
            BigDecimal price,
            String verdict,
            boolean willExceed,
            BigDecimal exceedBy,
            String category,
            BigDecimal balanceAfter,
            int affordabilityScore) {
        String priceLabel = "₹" + price.setScale(0, RoundingMode.HALF_UP).toPlainString();
        return switch (verdict) {
            case "GO_AHEAD" ->
                    "You can afford " + itemName + " (" + priceLabel
                            + "). Affordability score " + affordabilityScore
                            + "/100 — this looks manageable right now.";
            case "CONSIDER" -> {
                String extra = willExceed
                        ? " It would push " + category + " over budget by ₹"
                        + exceedBy.setScale(0, RoundingMode.HALF_UP).toPlainString() + "."
                        : balanceAfter.compareTo(BigDecimal.ZERO) < 0
                        ? " Your spendable balance would turn negative."
                        : " Leave some buffer for bills and goals.";
                yield "Possible but risky for " + itemName + " (" + priceLabel + ")." + extra;
            }
            default -> {
                String why = balanceAfter.compareTo(BigDecimal.ZERO) < 0
                        ? "you don't have enough spendable balance"
                        : willExceed
                        ? "it would blow past your " + category + " budget"
                        : "the affordability score is only " + affordabilityScore + "/100";
                yield "Not recommended to buy " + itemName + " now — " + why + ".";
            }
        };
    }

    private List<String> buildAlternatives(
            String itemName,
            BigDecimal price,
            String category,
            boolean willExceed,
            BigDecimal exceedBy,
            ImpactAnalysisDto impact,
            HealthScoreContext health,
            LocalDate paymentDate,
            PurchaseDecisionDetailDto decision) {
        List<String> alts = new ArrayList<>();

        if (paymentDate.getDayOfMonth() > 20 || willExceed) {
            alts.add("Wait until next month when your " + category + " budget resets");
        }

        BigDecimal cheaper = price.multiply(new BigDecimal("0.6")).setScale(0, RoundingMode.HALF_UP);
        if (cheaper.compareTo(new BigDecimal("500")) > 0) {
            alts.add("Buy a cheaper alternative under ₹" + cheaper.toPlainString());
        } else {
            alts.add("Look for a lower-cost option under ₹"
                    + price.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP).toPlainString());
        }

        int delayDays = impact.getSavingsGoalDelayDays();
        if (delayDays > 0 && !health.getGoals().isEmpty()) {
            SavingsGoal goal = health.getGoals().get(0);
            String weeks = delayDays >= 7
                    ? Math.max(1, delayDays / 7) + " week" + (delayDays >= 14 ? "s" : "")
                    : delayDays + " day" + (delayDays == 1 ? "" : "s");
            alts.add("This would only delay your " + goal.getName() + " goal by about " + weeks);
        } else if (!health.getGoals().isEmpty()) {
            alts.add("Pause this purchase and put ₹"
                    + price.setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + " toward " + health.getGoals().get(0).getName() + " instead");
        }

        if (decision.getRecommendedPurchaseDate() != null) {
            alts.add("Plan to buy on " + decision.getRecommendedPurchaseDate()
                    + " when cash flow looks healthier");
        }

        long daysToSalary = daysUntilSalary(health);
        if (daysToSalary > 0 && daysToSalary <= 21) {
            alts.add("Wait " + daysToSalary + " day" + (daysToSalary == 1 ? "" : "s")
                    + " until salary credit before buying " + itemName);
        }

        if (willExceed && exceedBy.signum() > 0) {
            alts.add("Trim other " + category + " spending by ₹"
                    + exceedBy.setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + " first, then buy");
        }

        // Keep 3–5 unique-ish items
        return alts.stream().distinct().limit(5).toList();
    }

    private static long daysUntilSalary(HealthScoreContext health) {
        if (health.getProfile() == null || health.getProfile().getSalaryDayOfMonth() == null) {
            return -1;
        }
        int salaryDay = health.getProfile().getSalaryDayOfMonth();
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        int day = Math.min(Math.max(salaryDay, 1), ym.lengthOfMonth());
        LocalDate next = ym.atDay(day);
        if (!next.isAfter(today)) {
            YearMonth nextMonth = ym.plusMonths(1);
            next = nextMonth.atDay(Math.min(day, nextMonth.lengthOfMonth()));
        }
        return ChronoUnit.DAYS.between(today, next);
    }

    private static double savingsRate(BigDecimal income, BigDecimal savings) {
        if (income == null || income.signum() <= 0) return 0;
        return savings.multiply(BigDecimal.valueOf(100))
                .divide(income, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static BigDecimal money(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
