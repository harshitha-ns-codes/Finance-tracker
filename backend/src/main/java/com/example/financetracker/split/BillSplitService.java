package com.example.financetracker.split;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.transaction.TransactionRequest;
import com.example.financetracker.transaction.TransactionService;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BillSplitService {

    private static final int OVERDUE_DAYS = 7;
    private static final String SETTLEMENT_CATEGORY = "Split Settlement";

    private final BillSplitRepository billSplitRepository;
    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public BillSplitService(
            BillSplitRepository billSplitRepository,
            TransactionService transactionService,
            CurrentUserService currentUserService) {
        this.billSplitRepository = billSplitRepository;
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public BillSplitDto create(BillSplitRequest request) {
        validateRequest(request);
        User user = currentUserService.getCurrentUser();
        BillSplit split = new BillSplit();
        split.setUser(user);
        applyRequest(split, request);
        split.setStatus(SplitStatus.PENDING);
        return BillSplitDto.from(billSplitRepository.save(split));
    }

    @Transactional(readOnly = true)
    public List<BillSplitDto> list(String statusFilter) {
        User user = currentUserService.getCurrentUser();
        List<BillSplit> splits;
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            splits = billSplitRepository.findByUserOrderByDateDescIdDesc(user);
        } else if ("SETTLED".equalsIgnoreCase(statusFilter)) {
            splits = billSplitRepository.findByUserAndStatusOrderByDateDescIdDesc(user, SplitStatus.SETTLED);
        } else if ("PENDING".equalsIgnoreCase(statusFilter)) {
            splits = billSplitRepository.findByUserAndStatusInOrderByDateDescIdDesc(
                    user, List.of(SplitStatus.PENDING, SplitStatus.REMINDED));
        } else {
            try {
                SplitStatus status = SplitStatus.valueOf(statusFilter.toUpperCase(Locale.ENGLISH));
                splits = billSplitRepository.findByUserAndStatusOrderByDateDescIdDesc(user, status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status filter. Use PENDING, SETTLED, or ALL");
            }
        }
        return splits.stream().map(BillSplitDto::from).toList();
    }

    @Transactional(readOnly = true)
    public BillSplitSummaryDto summary() {
        User user = currentUserService.getCurrentUser();
        List<BillSplit> splits = billSplitRepository.findByUserOrderByDateDescIdDesc(user);

        BigDecimal totalOwedToMe = BigDecimal.ZERO;
        BigDecimal totalIOwe = BigDecimal.ZERO;
        int pendingCount = 0;
        int overdueCount = 0;
        int owedToMeCount = 0;
        int iOweCount = 0;

        for (BillSplit split : splits) {
            if (split.getStatus() == SplitStatus.SETTLED) {
                continue;
            }
            pendingCount++;
            if (isOverdue(split)) {
                overdueCount++;
            }
            if (split.getSplitType() == SplitType.I_PAID) {
                totalOwedToMe = totalOwedToMe.add(split.getOtherPersonAmount());
                owedToMeCount++;
            } else {
                totalIOwe = totalIOwe.add(split.getOtherPersonAmount());
                iOweCount++;
            }
        }

        BillSplitSummaryDto dto = new BillSplitSummaryDto();
        dto.setTotalOwedToMe(totalOwedToMe);
        dto.setTotalIOwe(totalIOwe);
        dto.setNetBalance(totalOwedToMe.subtract(totalIOwe));
        dto.setPendingCount(pendingCount);
        dto.setOverdueCount(overdueCount);
        return dto;
    }

    @Transactional
    public BillSplitDto updateStatus(UUID id, SplitStatus newStatus) {
        if (newStatus != SplitStatus.REMINDED && newStatus != SplitStatus.SETTLED) {
            throw new IllegalArgumentException("status must be REMINDED or SETTLED");
        }
        User user = currentUserService.getCurrentUser();
        BillSplit split = billSplitRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Bill split not found"));
        if (split.getStatus() == SplitStatus.SETTLED) {
            throw new IllegalArgumentException("Split is already settled");
        }

        split.setStatus(newStatus);
        if (newStatus == SplitStatus.SETTLED) {
            LocalDate today = LocalDate.now();
            split.setSettledDate(today);
            createSettlementTransaction(split, today);
        }
        return BillSplitDto.from(billSplitRepository.save(split));
    }

    @Transactional
    public void delete(UUID id) {
        User user = currentUserService.getCurrentUser();
        BillSplit split = billSplitRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Bill split not found"));
        if (split.getStatus() != SplitStatus.PENDING) {
            throw new IllegalArgumentException("Only pending splits can be deleted");
        }
        billSplitRepository.delete(split);
    }

    static int daysSince(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(date, LocalDate.now()));
    }

    static boolean isOverdue(BillSplit split) {
        if (split.getStatus() == SplitStatus.SETTLED || split.getDate() == null) {
            return false;
        }
        return daysSince(split.getDate()) > OVERDUE_DAYS;
    }

    private void createSettlementTransaction(BillSplit split, LocalDate today) {
        TransactionRequest tx = new TransactionRequest();
        tx.setAmount(split.getOtherPersonAmount());
        tx.setDate(today);
        tx.setCategory(SETTLEMENT_CATEGORY);

        if (split.getSplitType() == SplitType.I_PAID) {
            tx.setType(TransactionType.INCOME);
            tx.setDescription(String.format(
                    "%s paid back for %s",
                    split.getOtherPersonName(),
                    split.getTitle()));
        } else {
            tx.setType(TransactionType.EXPENSE);
            tx.setDescription(String.format(
                    "Paid %s for %s",
                    split.getOtherPersonName(),
                    split.getTitle()));
        }
        transactionService.create(tx);
    }

    private void applyRequest(BillSplit split, BillSplitRequest request) {
        split.setTitle(request.getTitle().trim());
        split.setTotalAmount(request.getTotalAmount());
        split.setMyShare(request.getMyShare());
        split.setOtherPersonName(request.getOtherPersonName().trim());
        split.setOtherPersonAmount(request.getOtherPersonAmount());
        split.setSplitType(request.getSplitType());
        split.setCategory(request.getCategory().trim());
        split.setDate(request.getDate());
        split.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
    }

    private void validateRequest(BillSplitRequest request) {
        if (request.getTotalAmount() == null
                || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        if (request.getMyShare() == null || request.getMyShare().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("My share must be zero or positive");
        }
        if (request.getOtherPersonAmount() == null
                || request.getOtherPersonAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Other person's amount must be positive");
        }
        BigDecimal sum = request.getMyShare().add(request.getOtherPersonAmount());
        if (sum.compareTo(request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("My share and their share must equal the total bill");
        }
        if (request.getSplitType() == null) {
            throw new IllegalArgumentException("splitType is required");
        }
    }
}
