import { FormEvent, Fragment, useEffect, useMemo, useState } from "react";
import {
  CategoryBudgetItem,
  CategoryBudgetItemRequest,
  createBudgetItem,
  deleteBudgetItem,
  listBudgetItems,
  setBudgetCategorySpent,
  setBudgetItemPaid,
  updateBudgetItem
} from "../api";
import { BUDGET_CATEGORIES, CATEGORY_HINTS, BudgetCategory } from "../categories";
import { RecurringTransactionsPanel } from "../components/RecurringTransactionsPanel";
import { FinancialGoalsPanel } from "../components/FinancialGoalsPanel";

type PlanTab = "budget" | "recurring" | "goals";

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function formatInr(n: number): string {
  return `₹${Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
}

const emptyForm = (month: string): CategoryBudgetItemRequest => ({
  month,
  category: "Food",
  plannedAmount: 0,
  dueDate: `${month}-01`,
  description: "",
  fixed: false,
  paid: false
});

export function BudgetPage() {
  const [planTab, setPlanTab] = useState<PlanTab>("budget");
  const [month, setMonth] = useState(currentMonth);
  const [items, setItems] = useState<CategoryBudgetItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<CategoryBudgetItemRequest>(() => emptyForm(currentMonth()));
  const [spentEdits, setSpentEdits] = useState<Record<string, string>>({});
  const [savingSpent, setSavingSpent] = useState(false);
  const [spentSaveSuccess, setSpentSaveSuccess] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listBudgetItems(month);
      setItems(data);
    } catch (err: any) {
      setError(err.message || "Failed to load budget sheet");
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setForm(prev => ({
      ...prev,
      month,
      dueDate: prev.dueDate?.startsWith(month) ? prev.dueDate : `${month}-01`
    }));
    setSpentEdits({});
    setSpentSaveSuccess(false);
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [month]);

  const grouped = useMemo(() => {
    const map = new Map<string, CategoryBudgetItem[]>();
    for (const cat of BUDGET_CATEGORIES) map.set(cat, []);
    // Keep unknown/legacy categories (e.g. old Fixed Expenses) visible
    for (const item of items) {
      if (!map.has(item.category)) map.set(item.category, []);
      map.get(item.category)!.push(item);
    }
    const order = [
      ...BUDGET_CATEGORIES,
      ...[...map.keys()].filter(c => !(BUDGET_CATEGORIES as readonly string[]).includes(c))
    ];
    return order
      .map(cat => {
        const rows = map.get(cat) ?? [];
        return {
          category: cat,
          hint: (CATEGORY_HINTS as Record<string, string>)[cat] || "",
          rows,
          planned: rows.reduce((s, r) => s + Number(r.plannedAmount || 0), 0),
          spent: Number(rows[0]?.spentAmount || 0)
        };
      })
      .filter(g => g.rows.length > 0);
  }, [items]);

  const spentByCategory = useMemo(() => {
    const map = new Map<string, number>();
    for (const group of grouped) {
      const draft = spentEdits[group.category];
      map.set(
        group.category,
        draft !== undefined ? Number(draft) || 0 : group.spent
      );
    }
    return map;
  }, [grouped, spentEdits]);

  const displayTotals = useMemo(() => {
    const planned = items.reduce((s, i) => s + Number(i.plannedAmount || 0), 0);
    const spent = [...spentByCategory.values()].reduce((s, v) => s + v, 0);
    return { planned, spent, remaining: planned - spent };
  }, [items, spentByCategory]);

  const hasSpentChanges = useMemo(
    () =>
      Object.entries(spentEdits).some(([category, raw]) => {
        const group = grouped.find(g => g.category === category);
        if (!group) return false;
        return (Number(raw) || 0) !== group.spent;
      }),
    [spentEdits, grouped]
  );

  const resetForm = () => setForm(emptyForm(month));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createBudgetItem({
        ...form,
        month,
        plannedAmount: Number(form.plannedAmount) || 0,
        dueDate: form.dueDate || null,
        fixed: false,
        paid: false
      });
      resetForm();
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to save budget item");
    } finally {
      setSaving(false);
    }
  };

  const saveRow = async (
    item: CategoryBudgetItem,
    patch: Partial<CategoryBudgetItemRequest>
  ) => {
    setError(null);
    try {
      await updateBudgetItem(item.id, {
        month: item.month,
        category: (BUDGET_CATEGORIES as readonly string[]).includes(item.category)
          ? item.category
          : "Miscellaneous",
        plannedAmount: Number(item.plannedAmount) || 0,
        dueDate: item.dueDate || null,
        description: item.description || "",
        fixed: false,
        paid: item.paid,
        ...patch
      });
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to update sheet row");
      await load();
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this budget line?")) return;
    try {
      await deleteBudgetItem(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete");
    }
  };

  const togglePaid = async (item: CategoryBudgetItem) => {
    try {
      // optimistic UI
      setItems(prev =>
        prev.map(r => (r.id === item.id ? { ...r, paid: !r.paid } : r))
      );
      await setBudgetItemPaid(item.id, !item.paid);
    } catch (err: any) {
      setError(err.message || "Failed to update paid status");
      await load();
    }
  };

  const saveAllSpent = async () => {
    const pending = Object.entries(spentEdits).filter(([category, raw]) => {
      const group = grouped.find(g => g.category === category);
      if (!group) return false;
      return (Number(raw) || 0) !== group.spent;
    });
    if (pending.length === 0) return;

    setSavingSpent(true);
    setError(null);
    setSpentSaveSuccess(false);
    try {
      let updated = items;
      for (const [category, raw] of pending) {
        updated = await setBudgetCategorySpent(month, category, Number(raw) || 0);
      }
      setItems(updated);
      setSpentEdits({});
      setSpentSaveSuccess(true);
      window.setTimeout(() => setSpentSaveSuccess(false), 2500);
    } catch (err: any) {
      const msg = err.message || "Failed to save spent amounts";
      setError(
        msg.includes("Unauthorized") || msg.includes("log in")
          ? "Could not save spent amounts. Please log out and log in again. If the problem continues, restart the backend server."
          : msg
      );
    } finally {
      setSavingSpent(false);
    }
  };

  const spentDraft = (category: string, current: number) =>
    spentEdits[category] ?? String(current);

  return (
    <div className="page budget-page">
      <div className="budget-page-header">
        <div>
          <h1>Plan</h1>
          <p className="chart-subtitle">
            Budget sheet, recurring money moves, and savings goals — your financial plan in one place.
          </p>
        </div>
        {planTab === "budget" && (
          <label className="month-select">
            Month
            <input type="month" value={month} onChange={e => setMonth(e.target.value)} />
          </label>
        )}
      </div>

      <div className="page-main-tabs plan-tabs">
        <button
          type="button"
          className={planTab === "budget" ? "page-main-tab active" : "page-main-tab"}
          onClick={() => setPlanTab("budget")}
        >
          Budget
        </button>
        <button
          type="button"
          className={planTab === "recurring" ? "page-main-tab active" : "page-main-tab"}
          onClick={() => setPlanTab("recurring")}
        >
          Recurring
        </button>
        <button
          type="button"
          className={planTab === "goals" ? "page-main-tab active" : "page-main-tab"}
          onClick={() => setPlanTab("goals")}
        >
          Goals
        </button>
      </div>

      {planTab === "budget" && (
        <>
      <div className="budget-summary-strip">
        <div>
          <span className="label">Planned</span>
          <strong>{formatInr(displayTotals.planned)}</strong>
        </div>
        <div>
          <span className="label">Spent</span>
          <strong className="text-red">{formatInr(displayTotals.spent)}</strong>
        </div>
        <div>
          <span className="label">Remaining</span>
          <strong className={displayTotals.remaining < 0 ? "text-red" : "text-green"}>
            {formatInr(displayTotals.remaining)}
          </strong>
        </div>
      </div>

      <div className="budget-layout">
        <div className="card budget-form-card">
          <h2>Add budget line</h2>
          <form className="form" onSubmit={handleSubmit}>
            <label>
              Category
              <select
                value={form.category}
                onChange={e =>
                  setForm(prev => ({ ...prev, category: e.target.value }))
                }
              >
                {BUDGET_CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
              <span className="field-hint">
                {CATEGORY_HINTS[form.category as BudgetCategory]}
              </span>
            </label>
            <label>
              Budget amount (₹)
              <input
                type="number"
                step="0.01"
                min="0"
                value={form.plannedAmount}
                onChange={e =>
                  setForm(prev => ({
                    ...prev,
                    plannedAmount: Number(e.target.value) || 0
                  }))
                }
                required
              />
            </label>
            <label>
              Date
              <input
                type="date"
                value={form.dueDate || ""}
                onChange={e => setForm(prev => ({ ...prev, dueDate: e.target.value }))}
              />
            </label>
            <label>
              Description
              <input
                value={form.description || ""}
                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                placeholder="e.g. Rent, Netflix, SIP"
              />
            </label>
            {error && <div className="error">{error}</div>}
            <div className="form-actions">
              <button className="btn-primary" type="submit" disabled={saving}>
                {saving ? "Saving..." : "Add to sheet"}
              </button>
            </div>
          </form>
        </div>

        <div className="card sheet-card">
          <div className="sheet-toolbar">
            <div className="sheet-toolbar-main">
              <h2>Budget sheet</h2>
              <span className="sheet-hint">
                Edit cells directly · update Spent per category · save once to refresh dashboard &amp; health
              </span>
            </div>
            {items.length > 0 && (
              <div className="sheet-toolbar-actions">
                {error && <span className="sheet-save-error">{error}</span>}
                {hasSpentChanges && (
                  <span className="sheet-unsaved-badge">Unsaved spent changes</span>
                )}
                {spentSaveSuccess && (
                  <span className="sheet-save-success">
                    Saved — Overview &amp; Health now use these spent totals
                  </span>
                )}
                <button
                  type="button"
                  className="btn-primary sheet-spent-save"
                  disabled={!hasSpentChanges || savingSpent}
                  onClick={() => void saveAllSpent()}
                >
                  {savingSpent ? "Saving..." : "Save spent amounts"}
                </button>
              </div>
            )}
          </div>
          {loading ? (
            <p>Loading sheet...</p>
          ) : (
            <div className="sheet-scroll">
              <table className="sheet-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Date</th>
                    <th className="num">Budget</th>
                    <th className="num col-spent">Spent</th>
                    <th className="num">Left</th>
                    <th className="center">Paid</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="sheet-empty">
                        No lines yet — add a category budget on the left.
                      </td>
                    </tr>
                  ) : (
                    grouped.map(group => (
                      <Fragment key={group.category}>
                        {group.rows.map((row, idx) => {
                          const categorySpent =
                            spentByCategory.get(group.category) ?? group.spent;
                          const left = group.planned - categorySpent;
                          return (
                            <tr key={row.id} className="sheet-row">
                              <td className="sheet-cat">
                                {idx === 0 ? (
                                  <>
                                    <strong>{group.category}</strong>
                                    {group.hint && (
                                      <span className="sheet-cat-hint">{group.hint}</span>
                                    )}
                                  </>
                                ) : (
                                  <span className="sheet-cat-continued">↳</span>
                                )}
                              </td>
                              <td>
                                <input
                                  className="sheet-input"
                                  defaultValue={row.description || ""}
                                  key={`desc-${row.id}-${row.description}`}
                                  onBlur={e => {
                                    const next = e.target.value;
                                    if ((row.description || "") !== next) {
                                      void saveRow(row, { description: next });
                                    }
                                  }}
                                />
                              </td>
                              <td>
                                <input
                                  className="sheet-input"
                                  type="date"
                                  defaultValue={row.dueDate || ""}
                                  key={`date-${row.id}-${row.dueDate}`}
                                  onBlur={e => {
                                    const next = e.target.value || null;
                                    if ((row.dueDate || "") !== (next || "")) {
                                      void saveRow(row, { dueDate: next });
                                    }
                                  }}
                                />
                              </td>
                              <td className="num">
                                <input
                                  className="sheet-input num-input"
                                  type="number"
                                  step="0.01"
                                  min="0"
                                  defaultValue={row.plannedAmount}
                                  key={`amt-${row.id}-${row.plannedAmount}`}
                                  onBlur={e => {
                                    const next = Number(e.target.value) || 0;
                                    if (Number(row.plannedAmount) !== next) {
                                      void saveRow(row, { plannedAmount: next });
                                    }
                                  }}
                                />
                              </td>
                              <td className="num text-red col-spent">
                                {idx === 0 ? (
                                  <input
                                    className="sheet-input num-input spent-input"
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    placeholder="0"
                                    value={spentDraft(group.category, group.spent)}
                                    onChange={e =>
                                      setSpentEdits(prev => ({
                                        ...prev,
                                        [group.category]: e.target.value
                                      }))
                                    }
                                    onKeyDown={e => {
                                      if (e.key === "Enter" && hasSpentChanges) {
                                        e.preventDefault();
                                        void saveAllSpent();
                                      }
                                    }}
                                  />
                                ) : (
                                  "·"
                                )}
                              </td>
                              <td
                                className={`num ${left < 0 ? "text-red" : "text-green"}`}
                              >
                                {idx === 0 ? formatInr(left) : "·"}
                              </td>
                              <td className="center">
                                <label className="paid-tick" title={row.paid ? "Paid" : "Unpaid"}>
                                  <input
                                    type="checkbox"
                                    checked={row.paid}
                                    onChange={() => togglePaid(row)}
                                  />
                                  <span className="paid-tick-box" aria-hidden>
                                    {row.paid ? "✓" : ""}
                                  </span>
                                </label>
                              </td>
                              <td className="sheet-actions">
                                <button
                                  type="button"
                                  className="link-button danger"
                                  onClick={() => handleDelete(row.id)}
                                >
                                  Delete
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </Fragment>
                    ))
                  )}
                </tbody>
                {items.length > 0 && (
                  <tfoot>
                    <tr>
                      <td colSpan={3}>
                        <strong>Totals</strong>
                      </td>
                      <td className="num">
                        <strong>{formatInr(displayTotals.planned)}</strong>
                      </td>
                      <td className="num text-red">
                        <strong>{formatInr(displayTotals.spent)}</strong>
                      </td>
                      <td
                        className={`num ${
                          displayTotals.remaining < 0 ? "text-red" : "text-green"
                        }`}
                      >
                        <strong>{formatInr(displayTotals.remaining)}</strong>
                      </td>
                      <td colSpan={2}></td>
                    </tr>
                  </tfoot>
                )}
              </table>
            </div>
          )}
        </div>
      </div>
        </>
      )}

      {planTab === "recurring" && <RecurringTransactionsPanel />}

      {planTab === "goals" && (
        <div className="card">
          <FinancialGoalsPanel />
        </div>
      )}
    </div>
  );
}
