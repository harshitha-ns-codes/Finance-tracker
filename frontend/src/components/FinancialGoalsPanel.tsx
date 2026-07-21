import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  EmergencyFundRecommendation,
  FinancialGoal,
  GoalType,
  contributeToGoal,
  createFinancialGoal,
  deleteFinancialGoal,
  getEmergencyFundRecommendation,
  listFinancialGoals
} from "../api";
import { BUDGET_CATEGORIES } from "../categories";
import { CONFETTI_COLORS } from "../theme";

const GOAL_TYPES: { value: GoalType; label: string }[] = [
  { value: "EMERGENCY_FUND", label: "Emergency Fund" },
  { value: "VACATION", label: "Vacation Fund" },
  { value: "BIG_PURCHASE", label: "Big Purchase" },
  { value: "DEBT_PAYOFF", label: "Debt Payoff" },
  { value: "CUSTOM", label: "Custom" }
];

function formatInr(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function ConfettiOverlay({ show }: { show: boolean }) {
  if (!show) return null;
  return (
    <div className="goal-confetti-overlay" aria-hidden>
      {Array.from({ length: 40 }).map((_, i) => (
        <span
          key={i}
          className="goal-confetti-piece"
          style={{
            left: `${(i * 17) % 100}%`,
            animationDelay: `${(i % 10) * 0.05}s`,
            background: CONFETTI_COLORS[i % CONFETTI_COLORS.length]
          }}
        />
      ))}
    </div>
  );
}

type MilestoneToast = {
  goalName: string;
  message: string;
};

export function FinancialGoalsPanel() {
  const [goals, setGoals] = useState<FinancialGoal[]>([]);
  const [emergency, setEmergency] = useState<EmergencyFundRecommendation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confetti, setConfetti] = useState(false);
  const [milestoneToast, setMilestoneToast] = useState<MilestoneToast | null>(null);
  const [contributeId, setContributeId] = useState<number | null>(null);
  const [contributeAmount, setContributeAmount] = useState("");

  const [name, setName] = useState("");
  const [goalType, setGoalType] = useState<GoalType>("EMERGENCY_FUND");
  const [targetAmount, setTargetAmount] = useState("");
  const [currentAmount, setCurrentAmount] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [linkedCategory, setLinkedCategory] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [goalData, emergencyData] = await Promise.all([
        listFinancialGoals(),
        getEmergencyFundRecommendation()
      ]);
      setGoals(goalData);
      setEmergency(emergencyData);
    } catch (err: any) {
      setError(err.message || "Failed to load goals");
      setGoals([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const showMilestoneCelebration = (goal: FinancialGoal) => {
    const hit = goal.newlyAchievedMilestones?.[0];
    if (!hit) return;
    setConfetti(true);
    setMilestoneToast({ goalName: goal.name, message: hit.message || `Milestone ${hit.percent}%!` });
    window.setTimeout(() => setConfetti(false), 2500);
    window.setTimeout(() => setMilestoneToast(null), 4000);
  };

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await createFinancialGoal({
        name: name.trim(),
        goalType,
        targetAmount: Number(targetAmount) || 0,
        currentAmount: Number(currentAmount) || 0,
        targetDate: targetDate || undefined,
        linkedCategory: linkedCategory || undefined
      });
      showMilestoneCelebration(created);
      setShowForm(false);
      setName("");
      setTargetAmount("");
      setCurrentAmount("");
      setTargetDate("");
      setLinkedCategory("");
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to create goal");
    } finally {
      setSaving(false);
    }
  };

  const handleContribute = async (id: number) => {
    const amount = Number(contributeAmount);
    if (!amount || amount <= 0) return;
    setSaving(true);
    try {
      const updated = await contributeToGoal(id, amount);
      showMilestoneCelebration(updated);
      setContributeId(null);
      setContributeAmount("");
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to add contribution");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this goal?")) return;
    try {
      await deleteFinancialGoal(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete goal");
    }
  };

  const applyEmergencyTarget = () => {
    if (!emergency) return;
    setGoalType("EMERGENCY_FUND");
    setName("Emergency Fund");
    setTargetAmount(String(Math.round(emergency.recommendedTarget)));
    setShowForm(true);
  };

  return (
    <div className="financial-goals-panel">
      <ConfettiOverlay show={confetti} />
      {milestoneToast && (
        <div className="goal-milestone-toast">
          <strong>{milestoneToast.goalName}</strong>
          <p>{milestoneToast.message}</p>
        </div>
      )}

      <div className="goals-panel-header">
        <div>
          <h2>Financial Goals</h2>
          <p className="chart-subtitle">
            Set targets, track milestones, and celebrate progress along the way.
          </p>
        </div>
        <button type="button" className="btn-primary" onClick={() => setShowForm(v => !v)}>
          {showForm ? "Cancel" : "+ New goal"}
        </button>
      </div>

      {emergency && emergency.recommendedTarget > 0 && (
        <div className="goal-emergency-card">
          <span className="goal-emergency-icon">🛡️</span>
          <div>
            <p>{emergency.message}</p>
            <button type="button" className="link-button" onClick={applyEmergencyTarget}>
              Use recommended target
            </button>
          </div>
        </div>
      )}

      {error && <div className="error">{error}</div>}

      {showForm && (
        <form className="form goal-create-form card" onSubmit={handleCreate}>
          <label>
            Goal name
            <input value={name} onChange={e => setName(e.target.value)} required placeholder="Emergency Fund" />
          </label>
          <label>
            Goal type
            <select value={goalType} onChange={e => setGoalType(e.target.value as GoalType)}>
              {GOAL_TYPES.map(t => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Target (₹)
            <input
              type="number"
              min="1"
              step="0.01"
              value={targetAmount}
              onChange={e => setTargetAmount(e.target.value)}
              required
            />
          </label>
          <label>
            Starting amount (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={currentAmount}
              onChange={e => setCurrentAmount(e.target.value)}
            />
          </label>
          <label>
            Target date (optional)
            <input type="date" value={targetDate} onChange={e => setTargetDate(e.target.value)} />
          </label>
          <label>
            Link to savings category (auto-updates from income)
            <select value={linkedCategory} onChange={e => setLinkedCategory(e.target.value)}>
              <option value="">None</option>
              {BUDGET_CATEGORIES.map(c => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <button className="btn-primary" type="submit" disabled={saving}>
            {saving ? "Creating…" : "Create goal"}
          </button>
        </form>
      )}

      {loading ? (
        <p className="chart-subtitle">Loading goals…</p>
      ) : goals.length === 0 ? (
        <div className="goal-empty-state">
          <span className="goal-empty-emoji">🎯</span>
          <p>No goals yet — create one to start building momentum.</p>
        </div>
      ) : (
        <div className="goal-cards-list">
          {goals.map(goal => {
            const pct = Math.min(100, goal.progressPercent);
            return (
              <div key={goal.id} className="goal-card card">
                <div className="goal-card-header">
                  <div>
                    <strong>{goal.name}</strong>
                    <span className="goal-type-badge">
                      {GOAL_TYPES.find(t => t.value === goal.goalType)?.label ?? goal.goalType}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="link-button danger"
                    onClick={() => void handleDelete(goal.id)}
                  >
                    Delete
                  </button>
                </div>

                <div className="goal-progress-wrap">
                  <div className="goal-progress-bar">
                    <div className="goal-progress-fill" style={{ width: `${pct}%` }} />
                  </div>
                  <span className="goal-progress-label">
                    {formatInr(goal.currentAmount)} / {formatInr(goal.targetAmount)} ({pct.toFixed(0)}%)
                  </span>
                </div>

                <div className="goal-milestones">
                  {[25, 50, 75, 100].map(m => {
                    const achieved = goal.milestones?.find(ms => ms.percent === m)?.achieved;
                    return (
                      <span
                        key={m}
                        className={`goal-milestone-dot${achieved ? " achieved" : ""}`}
                        title={`${m}%`}
                      >
                        {m}%
                      </span>
                    );
                  })}
                </div>

                <div className="goal-meta-grid">
                  {goal.projectedCompletionDate && (
                    <div>
                      <span className="meta-label">Projected</span>
                      <span>{goal.projectedCompletionDate}</span>
                    </div>
                  )}
                  {goal.monthlyContributionNeeded != null && goal.monthlyContributionNeeded > 0 && (
                    <div>
                      <span className="meta-label">Need/month</span>
                      <span>{formatInr(goal.monthlyContributionNeeded)}</span>
                    </div>
                  )}
                  {goal.linkedCategory && (
                    <div>
                      <span className="meta-label">Linked</span>
                      <span>{goal.linkedCategory}</span>
                    </div>
                  )}
                </div>

                {goal.scheduleMessage && (
                  <p className="goal-schedule-msg">{goal.scheduleMessage}</p>
                )}
                {goal.insight && <p className="goal-insight">{goal.insight}</p>}

                {contributeId === goal.id ? (
                  <div className="goal-contribute-row">
                    <input
                      type="number"
                      min="1"
                      step="0.01"
                      placeholder="Amount (₹)"
                      value={contributeAmount}
                      onChange={e => setContributeAmount(e.target.value)}
                    />
                    <button
                      type="button"
                      className="btn-primary"
                      disabled={saving}
                      onClick={() => void handleContribute(goal.id)}
                    >
                      Add
                    </button>
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => setContributeId(null)}
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    className="btn-secondary goal-add-btn"
                    onClick={() => {
                      setContributeId(goal.id);
                      setContributeAmount("");
                    }}
                  >
                    + Add money
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
