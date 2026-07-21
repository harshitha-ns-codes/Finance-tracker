package com.example.financetracker.health.insight;

import com.example.financetracker.health.dto.CategoryScoreDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InsightGenerator {

    public List<String> positives(List<CategoryScoreDto> breakdown) {
        List<String> positives = new ArrayList<>();
        for (CategoryScoreDto cat : breakdown) {
            double ratio = cat.getMax() == 0 ? 0 : (double) cat.getScore() / cat.getMax();
            if (ratio >= 0.8 && !isNeutralPlaceholder(cat.getExplanation())) {
                positives.add(cat.getCategory() + ": " + cat.getExplanation());
            }
            for (String detail : cat.getDetails()) {
                String lower = detail.toLowerCase();
                if (lower.contains("within") || lower.contains("on schedule") || lower.contains("paid all")
                        || lower.contains("fully funded") || lower.contains("strong debt")) {
                    if (!positives.contains(detail)) {
                        positives.add(detail);
                    }
                }
            }
        }
        if (positives.isEmpty()) {
            positives.add("Keep logging transactions — clearer data unlocks stronger insights.");
        }
        return positives.stream().limit(6).toList();
    }

    public List<String> negatives(List<CategoryScoreDto> breakdown) {
        List<String> negatives = new ArrayList<>();
        for (CategoryScoreDto cat : breakdown) {
            double ratio = cat.getMax() == 0 ? 0 : (double) cat.getScore() / cat.getMax();
            if (ratio < 0.65) {
                negatives.add(cat.getCategory() + ": " + cat.getExplanation());
            }
            for (String detail : cat.getDetails()) {
                String lower = detail.toLowerCase();
                if (lower.contains("exceeded") || lower.contains("behind") || lower.contains("unpaid")
                        || lower.contains("unused") || lower.contains("duplicate") || lower.contains("rose")) {
                    if (!negatives.contains(detail)) {
                        negatives.add(detail);
                    }
                }
            }
        }
        return negatives.stream().limit(6).toList();
    }

    /** Skip soft defaults when the user has not provided enough data yet. */
    private static boolean isNeutralPlaceholder(String explanation) {
        if (explanation == null) return true;
        String lower = explanation.toLowerCase();
        return lower.contains("scored lightly")
                || lower.contains("scored neutrally")
                || lower.contains("scored cautiously")
                || lower.contains("not enough")
                || lower.contains("no ") && (lower.contains("tracked") || lower.contains("recorded")
                || lower.contains("set for") || lower.contains("active savings"));
    }
}
