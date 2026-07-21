package com.example.financetracker.analyzer;

import com.example.financetracker.transaction.NeedType;

import java.util.List;
import java.util.Locale;

public final class NeedTypeClassifier {

    private static final List<String> NEED_KEYWORDS = List.of(
            "rent", "housing", "groceries", "food", "transport", "utilities",
            "bills", "healthcare", "medicine", "insurance", "education"
    );

    private static final List<String> WANT_KEYWORDS = List.of(
            "shopping", "entertainment", "dining", "restaurants", "travel",
            "subscriptions", "netflix", "amazon prime", "hobbies", "games",
            "beauty", "gym"
    );

    private static final List<String> SAVING_KEYWORDS = List.of(
            "savings", "investment", "mutual fund", "sip", "emergency fund",
            "fixed deposit"
    );

    private NeedTypeClassifier() {
    }

    public static NeedType autoClassify(String category) {
        if (category == null || category.isBlank()) {
            return NeedType.UNCLASSIFIED;
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);

        for (String keyword : NEED_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return NeedType.NEED;
            }
        }
        for (String keyword : WANT_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return NeedType.WANT;
            }
        }
        for (String keyword : SAVING_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return NeedType.SAVING;
            }
        }
        return NeedType.UNCLASSIFIED;
    }

    public static NeedType effectiveType(NeedType stored, String category) {
        if (stored != null && stored != NeedType.UNCLASSIFIED) {
            return stored;
        }
        NeedType auto = autoClassify(category);
        if (auto == NeedType.UNCLASSIFIED) {
            return NeedType.WANT;
        }
        return auto;
    }

    public static boolean needsUserClassification(NeedType stored, String category) {
        if (stored != null && stored != NeedType.UNCLASSIFIED) {
            return false;
        }
        return autoClassify(category) == NeedType.UNCLASSIFIED;
    }
}
