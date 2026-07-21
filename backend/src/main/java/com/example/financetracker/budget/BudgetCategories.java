package com.example.financetracker.budget;

import java.util.List;

public final class BudgetCategories {

    public static final List<String> ALL = List.of(
            "Food",
            "Transport",
            "Shopping",
            "Entertainment",
            "Education",
            "Investments/Savings",
            "Miscellaneous"
    );

    private BudgetCategories() {
    }

    public static boolean isValid(String category) {
        return category != null && ALL.contains(category);
    }
}
