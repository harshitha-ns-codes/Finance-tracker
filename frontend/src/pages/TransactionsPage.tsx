import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  Transaction,
  TransactionRequest,
  TransactionType,
  createTransaction,
  deleteTransaction,
  exportTransactionsCsv,
  listTransactions,
  updateTransaction
} from "../api";
import { BUDGET_CATEGORIES } from "../categories";
import { RegretPendingReviews } from "../components/RegretPendingReviews";
import { RegretStatsSection } from "../components/RegretStatsSection";
import { BillSplitsTab } from "../components/BillSplitsTab";

const emptyForm: TransactionRequest = {
  amount: 0,
  type: "EXPENSE",
  category: "",
  description: "",
  date: new Date().toISOString().slice(0, 10)
};

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

type HistoryFilter = TransactionType;
type PageTab = "transactions" | "splits";

export function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [form, setForm] = useState<TransactionRequest>(emptyForm);
  const [useCustomCategory, setUseCustomCategory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [historyFilter, setHistoryFilter] = useState<HistoryFilter>("EXPENSE");
  const [exportMonth, setExportMonth] = useState(currentMonth);
  const [exporting, setExporting] = useState(false);
  const [regretStatsKey, setRegretStatsKey] = useState(0);
  const [pageTab, setPageTab] = useState<PageTab>("transactions");

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listTransactions();
      setTransactions(data);
    } catch (err: any) {
      setError(err.message || "Failed to load transactions");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const filteredTransactions = useMemo(
    () => transactions.filter(tx => tx.type === historyFilter),
    [transactions, historyFilter]
  );

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await updateTransaction(editing.id, form);
      } else {
        await createTransaction(form);
      }
      setHistoryFilter(form.type);
      setForm(emptyForm);
      setEditing(null);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to save transaction");
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (tx: Transaction) => {
    setEditing(tx);
    setUseCustomCategory(!BUDGET_CATEGORIES.includes(tx.category as any));
    setForm({
      amount: tx.amount,
      type: tx.type,
      category: tx.category,
      description: tx.description,
      date: tx.date
    });
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this transaction?")) return;
    try {
      await deleteTransaction(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete transaction");
    }
  };

  const handleChange = (field: keyof TransactionRequest, value: string) => {
    if (field === "amount") {
      setForm(prev => ({ ...prev, amount: Number(value) || 0 }));
    } else if (field === "type") {
      setForm(prev => ({ ...prev, type: value as TransactionType }));
    } else {
      setForm(prev => ({ ...prev, [field]: value }));
    }
  };

  const handleExport = async (all = false) => {
    setExporting(true);
    setError(null);
    try {
      const { blob, filename } = await exportTransactionsCsv(all ? undefined : exportMonth);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename || "transactions.csv";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.message || "Failed to export CSV");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="page">
      <div className="budget-page-header">
        <h1>Transactions</h1>
        {pageTab === "transactions" && (
          <div className="export-bar">
            <label className="month-select">
              Export month
              <input
                type="month"
                value={exportMonth}
                onChange={e => setExportMonth(e.target.value)}
              />
            </label>
            <button
              type="button"
              className="btn-secondary"
              disabled={exporting}
              onClick={() => handleExport(false)}
            >
              {exporting ? "Exporting..." : "Export CSV"}
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={exporting}
              onClick={() => handleExport(true)}
            >
              Export all
            </button>
          </div>
        )}
      </div>

      <div className="page-main-tabs">
        <button
          type="button"
          className={pageTab === "transactions" ? "page-main-tab active" : "page-main-tab"}
          onClick={() => setPageTab("transactions")}
        >
          Transactions
        </button>
        <button
          type="button"
          className={pageTab === "splits" ? "page-main-tab active" : "page-main-tab"}
          onClick={() => setPageTab("splits")}
        >
          Splits
        </button>
      </div>

      {pageTab === "splits" ? (
        <BillSplitsTab />
      ) : (
        <>
      <RegretPendingReviews onReviewed={() => setRegretStatsKey(k => k + 1)} />

      <div className="grid-2">
        <div className="card">
          <h2>{editing ? "Edit transaction" : "Add transaction"}</h2>
          <form className="form" onSubmit={handleSubmit}>
            <label>
              Type
              <select
                value={form.type}
                onChange={e => handleChange("type", e.target.value)}
              >
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </select>
            </label>
            <label>
              Amount
              <input
                type="number"
                step="0.01"
                value={form.amount}
                onChange={e => handleChange("amount", e.target.value)}
                required
              />
            </label>
            <label>
              Category
              <select
                value={form.category}
                onChange={e => handleChange("category", e.target.value)}
                required={!useCustomCategory}
                disabled={useCustomCategory}
              >
                <option value="">Select category</option>
                {BUDGET_CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </select>
            </label>
            <label className="check-row">
              <input
                type="checkbox"
                checked={useCustomCategory}
                onChange={e => {
                  const checked = e.target.checked;
                  setUseCustomCategory(checked);
                  setForm(prev => ({
                    ...prev,
                    category: checked ? prev.category : ""
                  }));
                }}
              />{" "}
              Use custom category
            </label>
            {useCustomCategory && (
              <label>
                Custom category
                <input
                  value={form.category}
                  onChange={e => handleChange("category", e.target.value)}
                  placeholder={form.type === "INCOME" ? "e.g. Salary, Freelance" : "e.g. Rent, Medicine"}
                  required
                />
              </label>
            )}
            <label>
              Description
              <input
                value={form.description || ""}
                onChange={e => handleChange("description", e.target.value)}
              />
            </label>
            <label>
              Date
              <input
                type="date"
                value={form.date}
                onChange={e => handleChange("date", e.target.value)}
                required
              />
            </label>
            {error && <div className="error">{error}</div>}
            <div className="form-actions">
              <button className="btn-primary" type="submit" disabled={saving}>
                {saving ? "Saving..." : editing ? "Update" : "Add"}
              </button>
              {editing && (
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setEditing(null);
                    setUseCustomCategory(false);
                    setForm(emptyForm);
                  }}
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>

        <div className="card">
          <div className="history-header">
            <h2>History</h2>
            <div className="tabs">
              <button
                type="button"
                className={historyFilter === "INCOME" ? "tab active" : "tab"}
                onClick={() => setHistoryFilter("INCOME")}
              >
                Income
              </button>
              <button
                type="button"
                className={historyFilter === "EXPENSE" ? "tab active" : "tab"}
                onClick={() => setHistoryFilter("EXPENSE")}
              >
                Expense
              </button>
            </div>
          </div>
          {loading ? (
            <p>Loading...</p>
          ) : filteredTransactions.length === 0 ? (
            <p>
              No {historyFilter === "INCOME" ? "income" : "expense"} transactions yet.
            </p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.map(tx => (
                  <tr key={tx.id}>
                    <td>{tx.date}</td>
                    <td>{tx.category}</td>
                    <td className={tx.type === "INCOME" ? "text-green" : "text-red"}>
                      {tx.type === "INCOME" ? "+" : "-"}₹{tx.amount.toFixed(2)}
                    </td>
                    <td>
                      <button
                        className="link-button"
                        onClick={() => startEdit(tx)}
                      >
                        Edit
                      </button>
                      <button
                        className="link-button danger"
                        onClick={() => handleDelete(tx.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <RegretStatsSection key={regretStatsKey} />
        </>
      )}
    </div>
  );
}
