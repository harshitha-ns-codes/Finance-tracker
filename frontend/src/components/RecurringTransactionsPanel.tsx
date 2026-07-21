import { FormEvent, useEffect, useState } from "react";
import {
  RecurringTransaction,
  RecurringTransactionRequest,
  TransactionType,
  createRecurring,
  deleteRecurring,
  listRecurring
} from "../api";
import { BUDGET_CATEGORIES } from "../categories";

function formatInr(n: number): string {
  return `₹${Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
}

const emptyForm = (): RecurringTransactionRequest => ({
  name: "",
  amount: 0,
  type: "EXPENSE",
  category: "Bills",
  dayOfMonth: 1
});

export function RecurringTransactionsPanel() {
  const [items, setItems] = useState<RecurringTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<RecurringTransactionRequest>(emptyForm);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await listRecurring());
    } catch (err: any) {
      setError(err.message || "Failed to load recurring transactions");
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createRecurring({
        ...form,
        name: form.name.trim(),
        amount: Number(form.amount) || 0,
        dayOfMonth: Math.min(31, Math.max(1, Number(form.dayOfMonth) || 1))
      });
      setForm(emptyForm());
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to save recurring transaction");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteRecurring(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete");
    }
  };

  return (
    <div className="card">
      <h2>Recurring transactions</h2>
      <p className="chart-subtitle">
        Salary, rent, EMIs, and other monthly items — used in your Home cash-flow forecast.
      </p>
      {error && <div className="error">{error}</div>}

      <form className="form plan-inline-form" onSubmit={handleSubmit}>
        <label>
          Name
          <input
            value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
            placeholder="Rent"
            required
          />
        </label>
        <label>
          Amount (₹)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={form.amount || ""}
            onChange={e => setForm(f => ({ ...f, amount: Number(e.target.value) || 0 }))}
            required
          />
        </label>
        <label>
          Type
          <select
            value={form.type}
            onChange={e => setForm(f => ({ ...f, type: e.target.value as TransactionType }))}
          >
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
        </label>
        <label>
          Category
          <select
            value={form.category}
            onChange={e => setForm(f => ({ ...f, category: e.target.value }))}
          >
            {BUDGET_CATEGORIES.map(c => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <label>
          Day of month
          <input
            type="number"
            min={1}
            max={31}
            value={form.dayOfMonth}
            onChange={e => setForm(f => ({ ...f, dayOfMonth: Number(e.target.value) || 1 }))}
          />
        </label>
        <button className="btn-primary" type="submit" disabled={saving}>
          {saving ? "Saving…" : "Add recurring"}
        </button>
      </form>

      {loading ? (
        <p className="chart-subtitle">Loading…</p>
      ) : items.length === 0 ? (
        <p className="chart-subtitle">No recurring items yet.</p>
      ) : (
        <div className="table-wrap">
          <table className="budget-sheet">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Category</th>
                <th className="num">Amount</th>
                <th className="center">Day</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td>{item.type}</td>
                  <td>{item.category}</td>
                  <td className="num">{formatInr(item.amount)}</td>
                  <td className="center">{item.dayOfMonth}</td>
                  <td className="sheet-actions">
                    <button
                      type="button"
                      className="link-button danger"
                      onClick={() => void handleDelete(item.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
