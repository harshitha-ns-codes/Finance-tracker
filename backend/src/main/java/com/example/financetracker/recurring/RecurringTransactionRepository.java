package com.example.financetracker.recurring;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(User user);

    Optional<RecurringTransaction> findByIdAndUser(Long id, User user);
}
