import { FormEvent, useEffect, useState } from "react";
import { SavingsGoal, createGoal, deleteGoal, listGoals } from "../api";

function formatInr(n: number): string {
  return `₹${Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

export function SavingsGoalsPanel() {
  const [goals, setGoals] = useState<SavingsGoal[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setGoals(await listGoals());
    } catch (err: any) {
      setError(err.message || "Failed to load savings goals");
      setGoals([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const addGoal = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    try {
      await createGoal({
        name: String(fd.get("name") || "Goal"),
        targetAmount: Number(fd.get("targetAmount")) || 0,
        currentAmount: Number(fd.get("currentAmount")) || 0,
        deadline: String(fd.get("deadline"))
      });
      e.currentTarget.reset();
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to add goal");
    }
  };

  const remove = async (id: number) => {
    try {
      await deleteGoal(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete goal");
    }
  };

  return (
    <div className="card">
      <h2>Savings goals</h2>
      <p className="chart-subtitle">
        Targets that feed your Advisor health score and purchase decisions.
      </p>
      {error && <div className="error">{error}</div>}

      <form className="form plan-inline-form" onSubmit={addGoal}>
        <label>
          Name
          <input name="name" required placeholder="Emergency fund" />
        </label>
        <label>
          Target (₹)
          <input name="targetAmount" type="number" min="1" required />
        </label>
        <label>
          Current (₹)
          <input name="currentAmount" type="number" min="0" defaultValue={0} />
        </label>
        <label>
          Deadline
          <input name="deadline" type="date" required />
        </label>
        <button className="btn-primary" type="submit">
          Add goal
        </button>
      </form>

      {loading ? (
        <p className="chart-subtitle">Loading…</p>
      ) : goals.length === 0 ? (
        <p className="chart-subtitle">No savings goals yet.</p>
      ) : (
        <ul className="mini-list goal-list">
          {goals.map(g => {
            const pct =
              g.targetAmount > 0
                ? Math.min(100, Math.round((g.currentAmount / g.targetAmount) * 100))
                : 0;
            return (
              <li key={g.id} className="goal-list-item">
                <div>
                  <strong>{g.name}</strong>
                  <span className="chart-subtitle">
                    {formatInr(g.currentAmount)} / {formatInr(g.targetAmount)} · by {g.deadline}
                  </span>
                  <div className="mini-progress">
                    <div style={{ width: `${pct}%` }} />
                  </div>
                </div>
                <button
                  type="button"
                  className="link-button danger"
                  onClick={() => void remove(g.id)}
                >
                  Remove
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
