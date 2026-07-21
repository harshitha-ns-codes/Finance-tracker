package com.example.financetracker.transaction;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.goal.FinancialGoalService;
import com.example.financetracker.user.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;
    private final FinancialGoalService financialGoalService;

    public TransactionService(
            TransactionRepository transactionRepository,
            CurrentUserService currentUserService,
            @Lazy FinancialGoalService financialGoalService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
        this.financialGoalService = financialGoalService;
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
        RegretService.applyRegretSchedule(tx);
        tx = transactionRepository.save(tx);
        financialGoalService.onTransactionCreated(tx);
        return tx;
    }

    @Transactional(readOnly = true)
    public List<Transaction> listForCurrentUser(LocalDate from, LocalDate to) {
        User user = currentUserService.getCurrentUser();
        if (from != null && to != null) {
            return transactionRepository.findByUserAndDateBetween(user, from, to);
        }
        return transactionRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String month) {
        User user = currentUserService.getCurrentUser();
        List<Transaction> transactions;
        if (month != null && !month.isBlank()) {
            YearMonth ym;
            try {
                ym = YearMonth.parse(month);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
            }
            transactions = transactionRepository.findByUserAndDateBetween(
                    user, ym.atDay(1), ym.atEndOfMonth());
        } else {
            transactions = transactionRepository.findByUser(user);
        }

        transactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate)
                        .thenComparing(Transaction::getId))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Category,Description,Amount\n");
        for (Transaction t : transactions) {
            csv.append(escapeCsv(t.getDate() != null ? t.getDate().toString() : "")).append(",")
                    .append(escapeCsv(t.getType() != null ? t.getType().name() : "")).append(",")
                    .append(escapeCsv(t.getCategory())).append(",")
                    .append(escapeCsv(t.getDescription())).append(",")
                    .append(t.getAmount() != null ? t.getAmount().toPlainString() : "0")
                    .append("\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String exportFilename(String month) {
        if (month == null || month.isBlank()) {
            return "transactions.csv";
        }
        try {
            YearMonth ym = YearMonth.parse(month);
            String monthName = ym.getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    .toLowerCase(Locale.ENGLISH);
            return "transactions-" + monthName + "-" + ym.getYear() + ".csv";
        } catch (Exception ex) {
            return "transactions.csv";
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
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

    @Transactional
    public Transaction classify(Long id, NeedType needType) {
        if (needType == null || needType == NeedType.UNCLASSIFIED) {
            throw new IllegalArgumentException("needType must be NEED, WANT, or SAVING");
        }
        User user = currentUserService.getCurrentUser();
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (tx.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Only expense transactions can be classified");
        }
        tx.setNeedType(needType);
        return transactionRepository.save(tx);
    }
}
