package com.example.financetracker.networth;

import com.example.financetracker.budget.CategoryBudgetService;
import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.health.model.FinancialProfile;
import com.example.financetracker.health.repo.FinancialProfileRepository;
import com.example.financetracker.transaction.Transaction;
import com.example.financetracker.transaction.TransactionRepository;
import com.example.financetracker.transaction.TransactionType;
import com.example.financetracker.user.User;
import com.example.financetracker.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NetWorthService {

    private final NetWorthPositionRepository positionRepository;
    private final NetWorthSnapshotRepository snapshotRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryBudgetService categoryBudgetService;
    private final FinancialProfileRepository financialProfileRepository;

    public NetWorthService(
            NetWorthPositionRepository positionRepository,
            NetWorthSnapshotRepository snapshotRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            CategoryBudgetService categoryBudgetService,
            FinancialProfileRepository financialProfileRepository) {
        this.positionRepository = positionRepository;
        this.snapshotRepository = snapshotRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.categoryBudgetService = categoryBudgetService;
        this.financialProfileRepository = financialProfileRepository;
    }

    @Transactional(readOnly = true)
    public NetWorthResponse getNetWorth() {
        User user = currentUserService.getCurrentUser();
        ensureMonthlySnapshot(user);
        return buildResponse(user);
    }

    @Transactional
    public NetWorthResponse saveNetWorth(NetWorthUpdateRequest request) {
        User user = currentUserService.getCurrentUser();
        persistPositions(user, request);
        NetWorthResponse response = buildResponse(user);
        saveSnapshot(user, YearMonth.now(), response);
        return response;
    }

    @Transactional
    public void ensureMonthlySnapshot(User user) {
        YearMonth current = YearMonth.now();
        if (snapshotRepository.findByUserAndSnapshotMonth(user, current).isEmpty()) {
            NetWorthResponse response = buildResponse(user);
            if (hasAnyData(response)) {
                saveSnapshot(user, current, response);
            }
        }
    }

    @Transactional
    public void snapshotAllUsersForMonth(YearMonth month) {
        for (User user : userRepository.findAll()) {
            try {
                NetWorthResponse response = buildResponse(user);
                if (hasAnyData(response)) {
                    saveSnapshot(user, month, response);
                }
            } catch (Exception ignored) {
                // skip users without valid state
            }
        }
    }

    private void persistPositions(User user, NetWorthUpdateRequest request) {
        upsertSingleton(user, PositionType.BANK_BALANCE, null,
                nz(request.getBankBalance()), request.isUseAutoBankBalance());
        upsertSingleton(user, PositionType.FIXED_DEPOSIT, null, nz(request.getFixedDeposits()), false);
        upsertSingleton(user, PositionType.INVESTMENTS, null, nz(request.getInvestments()), false);
        upsertSingleton(user, PositionType.STUDENT_LOAN, null, nz(request.getStudentLoan()), false);
        upsertSingleton(user, PositionType.CREDIT_CARD_DEBT, null, nz(request.getCreditCardDebt()), false);
        syncMultiItems(user, PositionType.PHYSICAL_ASSET, request.getPhysicalAssets());
        syncMultiItems(user, PositionType.MONEY_OWED, request.getMoneyOwed());
    }

    private void upsertSingleton(
            User user, PositionType type, String name, BigDecimal amount, boolean useAuto) {
        List<NetWorthPosition> existing = positionRepository.findByUser(user).stream()
                .filter(p -> p.getPositionType() == type)
                .toList();
        NetWorthPosition position = existing.isEmpty() ? new NetWorthPosition() : existing.get(0);
        position.setUser(user);
        position.setPositionType(type);
        position.setName(name);
        position.setAmount(amount);
        position.setUseAutoBalance(useAuto);
        positionRepository.save(position);
        for (int i = 1; i < existing.size(); i++) {
            positionRepository.delete(existing.get(i));
        }
    }

    private void syncMultiItems(User user, PositionType type, List<NamedAmountDto> items) {
        List<NetWorthPosition> existing = positionRepository.findByUser(user).stream()
                .filter(p -> p.getPositionType() == type)
                .toList();
        List<Long> keepIds = new ArrayList<>();
        if (items != null) {
            for (NamedAmountDto item : items) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }
                BigDecimal amount = nz(item.getAmount());
                NetWorthPosition position;
                if (item.getId() != null) {
                    position = existing.stream()
                            .filter(p -> item.getId().equals(p.getId()))
                            .findFirst()
                            .orElse(new NetWorthPosition());
                } else {
                    position = new NetWorthPosition();
                }
                position.setUser(user);
                position.setPositionType(type);
                position.setName(item.getName().trim());
                position.setAmount(amount);
                position.setUseAutoBalance(false);
                position = positionRepository.save(position);
                keepIds.add(position.getId());
            }
        }
        for (NetWorthPosition p : existing) {
            if (!keepIds.contains(p.getId())) {
                positionRepository.delete(p);
            }
        }
    }

    private NetWorthResponse buildResponse(User user) {
        List<NetWorthPosition> positions = positionRepository.findByUser(user);
        Map<PositionType, NetWorthPosition> singletons = new EnumMap<>(PositionType.class);
        List<NetWorthPosition> physicalAssets = new ArrayList<>();
        List<NetWorthPosition> moneyOwed = new ArrayList<>();

        for (NetWorthPosition p : positions) {
            if (p.getPositionType() == PositionType.PHYSICAL_ASSET) {
                physicalAssets.add(p);
            } else if (p.getPositionType() == PositionType.MONEY_OWED) {
                moneyOwed.add(p);
            } else {
                singletons.putIfAbsent(p.getPositionType(), p);
            }
        }

        BigDecimal autoBank = computeAutoBankBalance(user);
        NetWorthPosition bankPos = singletons.get(PositionType.BANK_BALANCE);
        boolean useAuto = bankPos == null || bankPos.isUseAutoBalance();
        BigDecimal bankAmount = useAuto ? autoBank : nz(bankPos != null ? bankPos.getAmount() : null);

        BigDecimal fixedDeposits = amountOf(singletons.get(PositionType.FIXED_DEPOSIT));
        BigDecimal investments = amountOf(singletons.get(PositionType.INVESTMENTS));
        BigDecimal physicalTotal = physicalAssets.stream()
                .map(NetWorthPosition::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal studentLoan = amountOf(singletons.get(PositionType.STUDENT_LOAN));
        BigDecimal creditCard = amountOf(singletons.get(PositionType.CREDIT_CARD_DEBT));
        BigDecimal moneyOwedTotal = moneyOwed.stream()
                .map(NetWorthPosition::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal liquidAssets = bankAmount.add(investments);
        BigDecimal fixedAssets = fixedDeposits.add(physicalTotal);
        BigDecimal totalAssets = liquidAssets.add(fixedAssets);
        BigDecimal totalLiabilities = studentLoan.add(creditCard).add(moneyOwedTotal);
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        NetWorthResponse response = new NetWorthResponse();
        response.setUseAutoBankBalance(useAuto);
        response.setAutoBankBalance(autoBank);
        response.setBankBalance(bankAmount);
        response.setFixedDeposits(fixedDeposits);
        response.setInvestments(investments);
        response.setPhysicalAssets(toNamedDtos(physicalAssets));
        response.setStudentLoan(studentLoan);
        response.setCreditCardDebt(creditCard);
        response.setMoneyOwed(toNamedDtos(moneyOwed));
        response.setTotalAssets(totalAssets);
        response.setTotalLiabilities(totalLiabilities);
        response.setNetWorth(netWorth);
        response.setLiquidAssets(liquidAssets);
        response.setFixedAssets(fixedAssets);
        response.setLiquidPercent(percent(liquidAssets, totalAssets));
        response.setFixedPercent(percent(fixedAssets, totalAssets));
        response.setAssetBreakdown(buildAssetBreakdown(
                bankAmount, investments, fixedDeposits, physicalAssets, totalAssets));
        response.setLiabilityBreakdown(buildLiabilityBreakdown(
                studentLoan, creditCard, moneyOwed, moneyOwedTotal, totalLiabilities));
        response.setHistory(snapshotRepository.findByUserOrderBySnapshotMonthAsc(user).stream()
                .map(NetWorthHistoryPointDto::from)
                .toList());
        applyMonthOverMonth(response);
        return response;
    }

    private void applyMonthOverMonth(NetWorthResponse response) {
        List<NetWorthHistoryPointDto> history = response.getHistory();
        if (history.size() < 2) {
            response.setMonthOverMonthChange(BigDecimal.ZERO);
            response.setMonthOverMonthMessage("Add monthly snapshots to track growth over time.");
            return;
        }
        NetWorthHistoryPointDto current = history.get(history.size() - 1);
        NetWorthHistoryPointDto previous = history.get(history.size() - 2);
        BigDecimal change = current.getNetWorth().subtract(previous.getNetWorth());
        response.setMonthOverMonthChange(change);
        if (change.compareTo(BigDecimal.ZERO) > 0) {
            response.setMonthOverMonthMessage(String.format(
                    Locale.ENGLISH,
                    "Your net worth grew %s this month.",
                    formatInr(change)));
        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
            response.setMonthOverMonthMessage(String.format(
                    Locale.ENGLISH,
                    "Your net worth fell %s this month — investigate what changed.",
                    formatInr(change.abs())));
        } else {
            response.setMonthOverMonthMessage("Your net worth held steady this month.");
        }
    }

    private void saveSnapshot(User user, YearMonth month, NetWorthResponse data) {
        NetWorthSnapshot snapshot = snapshotRepository
                .findByUserAndSnapshotMonth(user, month)
                .orElseGet(NetWorthSnapshot::new);
        snapshot.setUser(user);
        snapshot.setSnapshotMonth(month);
        snapshot.setTotalAssets(data.getTotalAssets());
        snapshot.setTotalLiabilities(data.getTotalLiabilities());
        snapshot.setNetWorth(data.getNetWorth());
        snapshot.setLiquidAssets(data.getLiquidAssets());
        snapshot.setFixedAssets(data.getFixedAssets());
        snapshot.setStudentLoan(data.getStudentLoan());
        snapshot.setCreditCardDebt(data.getCreditCardDebt());
        snapshot.setMoneyOwed(data.getMoneyOwed().stream()
                .map(NamedAmountDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        snapshot.setSnapshotDate(LocalDate.now());
        snapshotRepository.save(snapshot);
    }

    private BigDecimal computeAutoBankBalance(User user) {
        FinancialProfile profile = financialProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null && profile.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            return profile.getCurrentBalance();
        }
        YearMonth month = YearMonth.now();
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Transaction> txs = transactionRepository.findByUserAndDateBetween(user, from, to);
        BigDecimal income = txs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = categoryBudgetService.totalEffectiveSpent(user, month);
        return income.subtract(expenses);
    }

    private List<BreakdownItemDto> buildAssetBreakdown(
            BigDecimal bank,
            BigDecimal investments,
            BigDecimal fixedDeposits,
            List<NetWorthPosition> physical,
            BigDecimal total) {
        List<BreakdownItemDto> items = new ArrayList<>();
        items.add(new BreakdownItemDto("Bank balance", bank, percent(bank, total)));
        items.add(new BreakdownItemDto("Investments", investments, percent(investments, total)));
        items.add(new BreakdownItemDto("Fixed deposits", fixedDeposits, percent(fixedDeposits, total)));
        for (NetWorthPosition p : physical) {
            items.add(new BreakdownItemDto(
                    p.getName(),
                    p.getAmount(),
                    percent(p.getAmount(), total)));
        }
        return items.stream()
                .filter(i -> i.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private List<BreakdownItemDto> buildLiabilityBreakdown(
            BigDecimal studentLoan,
            BigDecimal creditCard,
            List<NetWorthPosition> moneyOwed,
            BigDecimal moneyOwedTotal,
            BigDecimal total) {
        List<BreakdownItemDto> items = new ArrayList<>();
        items.add(new BreakdownItemDto("Student loan", studentLoan, percent(studentLoan, total)));
        items.add(new BreakdownItemDto("Credit card debt", creditCard, percent(creditCard, total)));
        if (moneyOwed.size() == 1) {
            items.add(new BreakdownItemDto(
                    moneyOwed.get(0).getName(),
                    moneyOwed.get(0).getAmount(),
                    percent(moneyOwed.get(0).getAmount(), total)));
        } else if (moneyOwedTotal.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new BreakdownItemDto("Money owed", moneyOwedTotal, percent(moneyOwedTotal, total)));
        }
        return items.stream()
                .filter(i -> i.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private static List<NamedAmountDto> toNamedDtos(List<NetWorthPosition> positions) {
        return positions.stream().map(p -> {
            NamedAmountDto dto = new NamedAmountDto();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setAmount(p.getAmount());
            return dto;
        }).collect(Collectors.toList());
    }

    private static BigDecimal amountOf(NetWorthPosition position) {
        return position != null ? nz(position.getAmount()) : BigDecimal.ZERO;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static double percent(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static boolean hasAnyData(NetWorthResponse response) {
        return response.getTotalAssets().compareTo(BigDecimal.ZERO) > 0
                || response.getTotalLiabilities().compareTo(BigDecimal.ZERO) > 0;
    }

    private static String formatInr(BigDecimal amount) {
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
