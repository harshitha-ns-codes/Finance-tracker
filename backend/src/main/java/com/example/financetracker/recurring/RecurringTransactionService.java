package com.example.financetracker.recurring;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final CurrentUserService currentUserService;

    public RecurringTransactionService(
            RecurringTransactionRepository repository,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransaction> list() {
        User user = currentUserService.getCurrentUser();
        return repository.findByUserAndActiveTrueOrderByDayOfMonthAscIdAsc(user);
    }

    @Transactional
    public RecurringTransaction create(RecurringTransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        validate(request);
        RecurringTransaction item = new RecurringTransaction();
        apply(item, user, request);
        item.setActive(true);
        return repository.save(item);
    }

    @Transactional
    public RecurringTransaction update(Long id, RecurringTransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        validate(request);
        RecurringTransaction item = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found"));
        apply(item, user, request);
        return repository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        RecurringTransaction item = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found"));
        item.setActive(false);
        repository.save(item);
    }

    private void apply(RecurringTransaction item, User user, RecurringTransactionRequest request) {
        item.setUser(user);
        item.setName(request.getName().trim());
        item.setAmount(request.getAmount());
        item.setType(request.getType());
        item.setCategory(request.getCategory().trim());
        item.setDayOfMonth(Math.min(31, Math.max(1, request.getDayOfMonth())));
    }

    private void validate(RecurringTransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Type is required (INCOME or EXPENSE)");
        }
        if (request.getType() != TransactionType.INCOME && request.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Type must be INCOME or EXPENSE");
        }
    }
}
