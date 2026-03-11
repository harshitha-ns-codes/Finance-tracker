import { useEffect, useState } from "react";
import { Anomaly, DashboardSummary, getAnomalies, getDashboard } from "../api";

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [s, a] = await Promise.all([getDashboard(), getAnomalies()]);
        setSummary(s);
        setAnomalies(a);
      } catch (err: any) {
        setError(err.message || "Failed to load dashboard");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return <div className="page">Loading dashboard...</div>;
  }

  if (error) {
    return (
      <div className="page">
        <div className="error">{error}</div>
      </div>
    );
  }

  if (!summary) return null;

  return (
    <div className="page">
      <h1>Overview</h1>
      <div className="grid-3">
        <div className="card metric-card income">
          <span className="label">Total income</span>
          <span className="value">₹{summary.totalIncome.toFixed(2)}</span>
        </div>
        <div className="card metric-card expense">
          <span className="label">Total expenses</span>
          <span className="value">₹{summary.totalExpenses.toFixed(2)}</span>
        </div>
        <div className="card metric-card balance">
          <span className="label">Current balance</span>
          <span className="value">₹{summary.balance.toFixed(2)}</span>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <h2>Top spending</h2>
          {summary.topSpendingCategory ? (
            <p>
              You spend the most on <strong>{summary.topSpendingCategory}</strong> (
              ₹{summary.topSpendingAmount?.toFixed(2)}).
            </p>
          ) : (
            <p>No spending data yet.</p>
          )}
        </div>
        <div className="card">
          <h2>Budget status</h2>
          {summary.monthlyBudgetLimit ? (
            <>
              <p>
                This month&apos;s budget: <strong>₹{summary.monthlyBudgetLimit.toFixed(2)}</strong>
              </p>
              <p>
                Spent so far: <strong>₹{summary.monthlyExpenses?.toFixed(2)}</strong>
              </p>
              {summary.nearBudgetLimit ? (
                <p className="warning">You are close to your monthly budget limit.</p>
              ) : (
                <p className="success">You are within your monthly budget.</p>
              )}
            </>
          ) : (
            <p>No budget set for this month.</p>
          )}
        </div>
      </div>

      <div className="card">
        <h2>Potential anomalies</h2>
        {anomalies.length === 0 ? (
          <p>No unusual transactions detected.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {anomalies.map(a => (
                <tr key={a.transactionId}>
                  <td>{a.date}</td>
                  <td>{a.category}</td>
                  <td>₹{a.amount.toFixed(2)}</td>
                  <td>{a.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

