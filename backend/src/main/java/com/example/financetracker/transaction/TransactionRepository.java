package com.example.financetracker.transaction;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByUserAndDateBetween(User user, LocalDate start, LocalDate end);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.user = :user and t.type = 'INCOME'")
    BigDecimal totalIncomeForUser(User user);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.user = :user and t.type = 'EXPENSE'")
    BigDecimal totalExpensesForUser(User user);

    @Query("select t.category, coalesce(sum(t.amount), 0) from Transaction t where t.user = :user and t.type = 'EXPENSE' group by t.category order by sum(t.amount) desc")
    List<Object[]> topSpendingCategories(User user);

    @Query("select t.category, coalesce(sum(t.amount), 0) from Transaction t where t.user = :user and t.type = 'EXPENSE' and t.date between :from and :to group by t.category order by sum(t.amount) desc")
    List<Object[]> expensesByCategoryForPeriod(
            @Param("user") User user,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query(value = """
            SELECT EXTRACT(YEAR FROM t.date) AS yr,
                   EXTRACT(MONTH FROM t.date) AS mo,
                   COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS income,
                   COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS expenses
            FROM transactions t
            WHERE t.user_id = :userId
              AND t.date >= :fromDate
            GROUP BY EXTRACT(YEAR FROM t.date), EXTRACT(MONTH FROM t.date)
            ORDER BY yr, mo
            """, nativeQuery = true)
    List<Object[]> monthlyTotalsGrouped(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    List<Transaction> findByUserAndRegretStatusAndRegretReviewDateLessThanEqualOrderByRegretReviewDateAsc(
            User user, RegretStatus regretStatus, LocalDate regretReviewDate);

    List<Transaction> findByUserAndRegretStatusIn(User user, List<RegretStatus> statuses);
}

