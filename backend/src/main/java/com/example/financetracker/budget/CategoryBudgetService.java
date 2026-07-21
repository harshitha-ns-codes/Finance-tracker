package com.example.financetracker.budget;

import com.example.financetracker.common.CurrentUserService;
import com.example.financetracker.common.NotFoundException;
import com.example.financetracker.transaction.TransactionRepository;
import com.example.financetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryBudgetService {

    private final CategoryBudgetItemRepository itemRepository;
    private final CategoryBudgetSpentOverrideRepository spentOverrideRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public CategoryBudgetService(CategoryBudgetItemRepository itemRepository,
                                 CategoryBudgetSpentOverrideRepository spentOverrideRepository,
                                 BudgetRepository budgetRepository,
                                 TransactionRepository transactionRepository,
                                 CurrentUserService currentUserService) {
        this.itemRepository = itemRepository;
        this.spentOverrideRepository = spentOverrideRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> effectiveSpentByCategory(User user, YearMonth month) {
        return effectiveSpentMap(user, month);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalEffectiveSpent(User user, YearMonth month) {
        return effectiveSpentByCategory(user, month).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPlannedForMonth(User user, YearMonth month) {
        return itemRepository.findByUserAndMonthOrderByCategoryAscIdAsc(user, month).stream()
                .map(CategoryBudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<CategoryBudgetItemView> listForMonth(String monthStr) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(monthStr);
        Map<String, BigDecimal> spentByCategory = effectiveSpentMap(user, month);

        List<CategoryBudgetItemView> views = new ArrayList<>();
        for (CategoryBudgetItem item : itemRepository.findByUserAndMonthOrderByCategoryAscIdAsc(user, month)) {
            views.add(toView(item, spentByCategory.getOrDefault(item.getCategory(), BigDecimal.ZERO)));
        }
        return views;
    }

    @Transactional
    public CategoryBudgetItemView create(CategoryBudgetItemRequest request) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(request.getMonth());
        validate(request);

        CategoryBudgetItem item = new CategoryBudgetItem();
        apply(item, user, month, request);
        item = itemRepository.save(item);
        syncMonthlyBudget(user, month);

        BigDecimal spent = effectiveSpentMap(user, month).getOrDefault(item.getCategory(), BigDecimal.ZERO);
        return toView(item, spent);
    }

    @Transactional
    public CategoryBudgetItemView update(Long id, CategoryBudgetItemRequest request) {
        User user = currentUserService.getCurrentUser();
        validate(request);
        CategoryBudgetItem item = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Budget item not found"));
        YearMonth month = parseMonth(request.getMonth());
        apply(item, user, month, request);
        item = itemRepository.save(item);
        syncMonthlyBudget(user, month);

        BigDecimal spent = effectiveSpentMap(user, month).getOrDefault(item.getCategory(), BigDecimal.ZERO);
        return toView(item, spent);
    }

    @Transactional
    public CategoryBudgetItemView setPaid(Long id, boolean paid) {
        User user = currentUserService.getCurrentUser();
        CategoryBudgetItem item = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Budget item not found"));
        item.setPaid(paid);
        item = itemRepository.save(item);
        BigDecimal spent = effectiveSpentMap(user, item.getMonth())
                .getOrDefault(item.getCategory(), BigDecimal.ZERO);
        return toView(item, spent);
    }

    @Transactional
    public List<CategoryBudgetItemView> setCategorySpent(String monthStr, String category, BigDecimal spentAmount) {
        User user = currentUserService.getCurrentUser();
        YearMonth month = parseMonth(monthStr);
        if (!BudgetCategories.isValid(category)) {
            throw new IllegalArgumentException(
                    "Invalid category. Allowed: " + String.join(", ", BudgetCategories.ALL));
        }
        if (spentAmount == null || spentAmount.signum() < 0) {
            throw new IllegalArgumentException("Spent amount must be zero or positive");
        }

        CategoryBudgetSpentOverride override = spentOverrideRepository
                .findByUserAndMonthAndCategory(user, month, category)
                .orElseGet(CategoryBudgetSpentOverride::new);
        override.setUser(user);
        override.setMonth(month);
        override.setCategory(category);
        override.setSpentAmount(spentAmount);
        spentOverrideRepository.save(override);

        // Keep monthly Budget row in sync so Dashboard / Health always see a limit + spent
        syncMonthlyBudget(user, month);

        return listForMonth(month.toString());
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        CategoryBudgetItem item = itemRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Budget item not found"));
        YearMonth month = item.getMonth();
        itemRepository.delete(item);
        syncMonthlyBudget(user, month);
    }

    private void apply(CategoryBudgetItem item, User user, YearMonth month, CategoryBudgetItemRequest request) {
        item.setUser(user);
        item.setMonth(month);
        item.setCategory(request.getCategory());
        item.setPlannedAmount(request.getPlannedAmount());
        item.setDueDate(request.getDueDate());
        item.setDescription(request.getDescription());
        item.setFixed(false);
        item.setPaid(request.isPaid());
    }

    private void validate(CategoryBudgetItemRequest request) {
        if (!BudgetCategories.isValid(request.getCategory())) {
            throw new IllegalArgumentException(
                    "Invalid category. Allowed: " + String.join(", ", BudgetCategories.ALL));
        }
        if (request.getPlannedAmount() == null || request.getPlannedAmount().signum() < 0) {
            throw new IllegalArgumentException("Planned amount must be zero or positive");
        }
    }

    private void syncMonthlyBudget(User user, YearMonth month) {
        BigDecimal total = itemRepository.findByUserAndMonthOrderByCategoryAscIdAsc(user, month).stream()
                .map(CategoryBudgetItem::getPlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Budget budget = budgetRepository.findByUserAndMonth(user, month).orElseGet(Budget::new);
        budget.setUser(user);
        budget.setMonth(month);
        budget.setMonthlyLimit(total);
        budgetRepository.save(budget);
    }

    private Map<String, BigDecimal> spentMap(User user, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Object[]> rows = transactionRepository.expensesByCategoryForPeriod(user, from, to);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            String category = row[0] != null ? row[0].toString() : "Miscellaneous";
            BigDecimal amount = row[1] instanceof BigDecimal bd
                    ? bd
                    : new BigDecimal(row[1].toString());
            map.put(category, amount);
        }
        return map;
    }

    private Map<String, BigDecimal> effectiveSpentMap(User user, YearMonth month) {
        Map<String, BigDecimal> map = spentMap(user, month);
        for (CategoryBudgetSpentOverride override : spentOverrideRepository.findByUserAndMonth(user, month)) {
            map.put(override.getCategory(), override.getSpentAmount());
        }
        return map;
    }

    private CategoryBudgetItemView toView(CategoryBudgetItem item, BigDecimal spent) {
        CategoryBudgetItemView view = new CategoryBudgetItemView();
        view.setId(item.getId());
        view.setMonth(item.getMonth().toString());
        view.setCategory(item.getCategory());
        view.setPlannedAmount(item.getPlannedAmount());
        view.setSpentAmount(spent);
        view.setRemainingAmount(item.getPlannedAmount().subtract(spent));
        view.setDueDate(item.getDueDate());
        view.setDescription(item.getDescription());
        view.setFixed(item.isFixed());
        view.setPaid(item.isPaid());
        return view;
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid month format. Expected yyyy-MM");
        }
    }
}
