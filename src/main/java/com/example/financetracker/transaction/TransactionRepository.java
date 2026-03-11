package com.example.financetracker.transaction;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    Optional<Transaction> findByIdAndUser(Long id, User user);
}

