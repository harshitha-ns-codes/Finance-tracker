package com.example.financetracker.budget;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface CategoryBudgetItemRepository extends JpaRepository<CategoryBudgetItem, Long> {

    List<CategoryBudgetItem> findByUserAndMonthOrderByCategoryAscIdAsc(User user, YearMonth month);

    List<CategoryBudgetItem> findByUser(User user);

    Optional<CategoryBudgetItem> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);
}
