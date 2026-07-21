package com.example.financetracker.health.advisory.config;

import com.example.financetracker.health.advisory.prediction.HistoricalProjectionStrategy;
import com.example.financetracker.health.advisory.prediction.PaceProjectionStrategy;
import com.example.financetracker.health.advisory.prediction.SpendingPredictionStrategy;
import com.example.financetracker.health.advisory.prediction.WeightedCompositePredictionStrategy;
import com.example.financetracker.health.config.HealthScoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdvisoryEngineConfig {

    @Bean
    public PaceProjectionStrategy paceProjectionStrategy() {
        return new PaceProjectionStrategy();
    }

    @Bean
    public HistoricalProjectionStrategy historicalProjectionStrategy() {
        return new HistoricalProjectionStrategy();
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public SpendingPredictionStrategy spendingPredictionStrategy(
            PaceProjectionStrategy pace,
            HistoricalProjectionStrategy historical,
            HealthScoreProperties props) {
        return new WeightedCompositePredictionStrategy(pace, historical, props);
    }
}
