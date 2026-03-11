import { FormEvent, useEffect, useState } from "react";
import { Budget, getBudget, upsertBudget } from "../api";

export function BudgetPage() {
  const [month, setMonth] = useState<string>(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  });
  const [limit, setLimit] = useState<number>(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [budget, setBudget] = useState<Budget | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const b = await getBudget(month);
      setBudget(b);
      setLimit(b.monthlyLimit);
    } catch (err: any) {
      // 404 is okay: no budget set
      if (!String(err.message || "").includes("404")) {
        setError(err.message || "Failed to load budget");
      }
      setBudget(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [month]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const b = await upsertBudget(month, limit);
      setBudget(b);
    } catch (err: any) {
      setError(err.message || "Failed to save budget");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <h1>Monthly budget</h1>
      <div className="card">
        <form className="form" onSubmit={handleSubmit}>
          <label>
            Month
            <input
              type="month"
              value={month}
              onChange={e => setMonth(e.target.value)}
            />
          </label>
          <label>
            Monthly limit (₹)
            <input
              type="number"
              step="0.01"
              value={limit}
              onChange={e => setLimit(Number(e.target.value) || 0)}
            />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn-primary" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save budget"}
          </button>
        </form>
        {loading ? (
          <p>Loading current budget...</p>
        ) : budget ? (
          <p className="mt">
            Current budget for <strong>{budget.month}</strong>:{" "}
            <strong>₹{budget.monthlyLimit.toFixed(2)}</strong>
          </p>
        ) : (
          <p className="mt">No budget set for this month.</p>
        )}
      </div>
    </div>
  );
}

