package com.example.financetracker.transaction;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository,
                              CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Transaction create(TransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setCategory(request.getCategory());
        tx.setDescription(request.getDescription());
        tx.setDate(request.getDate());
        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<Transaction> listForCurrentUser(LocalDate from, LocalDate to) {
        User user = currentUserService.getCurrentUser();
        if (from != null && to != null) {
            return transactionRepository.findByUserAndDateBetween(user, from, to);
        }
        return transactionRepository.findByUser(user);
    }

    @Transactional
    public Transaction update(Long id, TransactionRequest request) {
        User user = currentUserService.getCurrentUser();
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setCategory(request.getCategory());
        tx.setDescription(request.getDescription());
        tx.setDate(request.getDate());
        return transactionRepository.save(tx);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        transactionRepository.delete(tx);
    }
}

