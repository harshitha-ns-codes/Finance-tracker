import { FormEvent, useEffect, useState } from "react";
import {
  Transaction,
  TransactionRequest,
  TransactionType,
  createTransaction,
  deleteTransaction,
  listTransactions,
  updateTransaction
} from "../api";

const emptyForm: TransactionRequest = {
  amount: 0,
  type: "EXPENSE",
  category: "",
  description: "",
  date: new Date().toISOString().slice(0, 10)
};

export function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [form, setForm] = useState<TransactionRequest>(emptyForm);
  const [saving, setSaving] = useState(false);

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

  return (
    <div className="page">
      <h1>Transactions</h1>

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
              <input
                value={form.category}
                onChange={e => handleChange("category", e.target.value)}
                required
              />
            </label>
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
          <h2>History</h2>
          {loading ? (
            <p>Loading...</p>
          ) : transactions.length === 0 ? (
            <p>No transactions yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Category</th>
                  <th>Amount</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {transactions.map(tx => (
                  <tr key={tx.id}>
                    <td>{tx.date}</td>
                    <td>{tx.type}</td>
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
    </div>
  );
}

