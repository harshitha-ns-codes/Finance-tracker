package com.example.financetracker.budget;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CategoryBudgetSpentOverrideRepository extends JpaRepository<CategoryBudgetSpentOverride, Long> {

    List<CategoryBudgetSpentOverride> findByUserAndMonth(User user, YearMonth month);

    Optional<CategoryBudgetSpentOverride> findByUserAndMonthAndCategory(User user, YearMonth month, String category);
}
