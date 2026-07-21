package com.example.financetracker.health.advisory.prediction;

import com.example.financetracker.health.advisory.FinancialAdvisoryContext;

import java.math.BigDecimal;

/** Strategy for projecting end-of-month category spend. */
public interface SpendingPredictionStrategy {

    BigDecimal predictCategory(String category, FinancialAdvisoryContext ctx);
}
