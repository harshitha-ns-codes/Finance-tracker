import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  SalaryIntelligence,
  listTransactions,
  getSalaryIntelligence,
  saveSalaryProfile
} from "../api";

function formatInr(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function ordinal(day: number): string {
  if (day >= 11 && day <= 13) return `${day}th`;
  switch (day % 10) {
    case 1:
      return `${day}st`;
    case 2:
      return `${day}nd`;
    case 3:
      return `${day}rd`;
    default:
      return `${day}th`;
  }
}

function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function allocationDismissKey(): string {
  const d = new Date();
  return `allocation_dismissed_${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function isAllocationAutoDismissed(salaryDay: number, todayDay: number): boolean {
  try {
    if (localStorage.getItem(allocationDismissKey()) === "true") return true;
  } catch {
    // ignore
  }
  return todayDay > salaryDay + 3;
}

export function ForecastPage() {
  const [intel, setIntel] = useState<SalaryIntelligence | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editOpen, setEditOpen] = useState(false);
  const [salaryDayInput, setSalaryDayInput] = useState("1");
  const [salaryAmountInput, setSalaryAmountInput] = useState("");
  const [todaySpent, setTodaySpent] = useState(0);
  const [allocationHidden, setAllocationHidden] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSalaryIntelligence();
      setIntel(data);
      if (data.configured && data.salaryDay != null && data.salaryAmount != null) {
        setSalaryDayInput(String(data.salaryDay));
        setSalaryAmountInput(String(data.salaryAmount));
        if (data.salaryDay != null && data.todayDayOfMonth != null) {
          setAllocationHidden(isAllocationAutoDismissed(data.salaryDay, data.todayDayOfMonth));
        }
      }
    } catch (err: any) {
      const msg = err.message || "Failed to load forecast";
      if (/unauthorized|log in again|session expired/i.test(msg)) {
        setError("Session expired. Log out and sign in again, then retry.");
      } else {
        setError(msg);
      }
      setIntel({ configured: false });
    }

    try {
      const today = todayIso();
      const txs = await listTransactions(today, today);
      const spent = txs
        .filter(t => t.type === "EXPENSE")
        .reduce((s, t) => s + Number(t.amount || 0), 0);
      setTodaySpent(spent);
    } catch {
      setTodaySpent(0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSaveSalary = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await saveSalaryProfile({
        salaryDay: Number(salaryDayInput) || 1,
        salaryAmount: Number(salaryAmountInput) || 0
      });
      setEditOpen(false);
      await load();
    } catch (err: any) {
      const msg = err.message || "Failed to save salary settings";
      if (/unauthorized|log in again|session expired/i.test(msg)) {
        setError("Session expired. Log out and sign in again, then retry.");
      } else {
        setError(msg);
      }
    } finally {
      setSaving(false);
    }
  };

  const dailyBudget = intel?.dailyBudget ?? 0;
  const remainingToday = dailyBudget - todaySpent;
  const dailyPct = dailyBudget > 0 ? (todaySpent / dailyBudget) * 100 : 0;

  const monthProgress = useMemo(() => {
    if (!intel?.todayDayOfMonth || !intel?.daysInMonth) return 0;
    return Math.min(100, (intel.todayDayOfMonth / intel.daysInMonth) * 100);
  }, [intel]);

  const dismissAllocation = () => {
    try {
      localStorage.setItem(allocationDismissKey(), "true");
    } catch {
      // ignore
    }
    setAllocationHidden(true);
  };

  const showAllocation =
    intel?.configured &&
    intel.showAllocation &&
    intel.allocationPlan &&
    !allocationHidden;

  const allocationTotal = useMemo(() => {
    if (!intel?.allocationPlan) return 0;
    return intel.allocationPlan.breakdown.reduce((s, b) => s + b.amount, 0);
  }, [intel]);

  const allocationRemaining =
    (intel?.allocationPlan?.salaryAmount ?? 0) - allocationTotal;

  return (
    <div className="page forecast-page">
      <div className="budget-page-header">
        <div>
          <h1>Forecast</h1>
          <p className="chart-subtitle">Forward-looking financial intelligence</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {loading ? (
        <div className="card">Loading forecast…</div>
      ) : (
        <>
          {!intel?.configured ? (
            <div className="card salary-config-card salary-config-empty">
              <p className="salary-config-prompt">
                Set your salary day to unlock salary-cycle intelligence
              </p>
              <form className="salary-config-form" onSubmit={handleSaveSalary}>
                <label>
                  Salary arrives on day
                  <input
                    type="number"
                    min={1}
                    max={31}
                    placeholder="e.g. 1"
                    value={salaryDayInput}
                    onChange={e => setSalaryDayInput(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Monthly salary amount (₹)
                  <input
                    type="number"
                    min={1}
                    step={0.01}
                    placeholder="e.g. 25000"
                    value={salaryAmountInput}
                    onChange={e => setSalaryAmountInput(e.target.value)}
                    required
                  />
                </label>
                <button className="btn-primary" type="submit" disabled={saving}>
                  {saving ? "Saving…" : "Save"}
                </button>
              </form>
            </div>
          ) : (
            <>
              <div className="card salary-config-card salary-config-set">
                {!editOpen ? (
                  <button
                    type="button"
                    className="salary-config-summary"
                    onClick={() => setEditOpen(true)}
                  >
                    Salary: {formatInr(intel.salaryAmount ?? 0)} on the{" "}
                    {ordinal(intel.salaryDay ?? 1)} ✎
                  </button>
                ) : (
                  <form className="salary-config-form" onSubmit={handleSaveSalary}>
                    <label>
                      Salary arrives on day
                      <input
                        type="number"
                        min={1}
                        max={31}
                        value={salaryDayInput}
                        onChange={e => setSalaryDayInput(e.target.value)}
                        required
                      />
                    </label>
                    <label>
                      Monthly salary amount (₹)
                      <input
                        type="number"
                        min={1}
                        step={0.01}
                        value={salaryAmountInput}
                        onChange={e => setSalaryAmountInput(e.target.value)}
                        required
                      />
                    </label>
                    <div className="salary-config-actions">
                      <button className="btn-primary" type="submit" disabled={saving}>
                        {saving ? "Saving…" : "Save"}
                      </button>
                      <button
                        className="btn-secondary"
                        type="button"
                        onClick={() => setEditOpen(false)}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                )}
              </div>

              {intel.zone === "PRE_SALARY" && (
                <div className="card salary-intel-card pre-salary">
                  <div className="salary-countdown-wrap">
                    <div
                      className={`salary-countdown-ring ${
                        (intel.daysUntilSalary ?? 0) < 5 ? "urgent" : "calm"
                      }`}
                      style={{ ["--progress" as string]: `${monthProgress}%` }}
                    >
                      <div className="salary-countdown-inner">
                        <span className="salary-countdown-number">
                          {intel.daysUntilSalary}
                        </span>
                        <span className="salary-countdown-label">days until salary</span>
                      </div>
                    </div>
                  </div>

                  <div className="salary-stat-pills">
                    <span>Current Balance: {formatInr(intel.currentBalance ?? 0)}</span>
                    <span>Daily Budget: {formatInr(intel.dailyBudget ?? 0)}</span>
                    <span>Salary on {ordinal(intel.salaryDay ?? 1)}</span>
                  </div>

                  <div
                    className={`salary-insight-box ${
                      (intel.daysUntilSalary ?? 99) <= 3
                        ? "tone-warning"
                        : (intel.daysUntilSalary ?? 99) <= 7
                          ? "tone-caution"
                          : "tone-comfort"
                    }`}
                  >
                    {(intel.daysUntilSalary ?? 0) <= 3 && (
                      <p>
                        ⚠️ Salary arrives in {intel.daysUntilSalary} days. You have{" "}
                        {formatInr(intel.currentBalance ?? 0)} left. Limit spending to{" "}
                        {formatInr(intel.dailyBudget ?? 0)}/day to avoid going negative.
                      </p>
                    )}
                    {(intel.daysUntilSalary ?? 0) > 3 &&
                      (intel.daysUntilSalary ?? 0) <= 7 && (
                        <p>
                          Salary arrives in {intel.daysUntilSalary} days. Your daily budget until
                          then: {formatInr(intel.dailyBudget ?? 0)}. Stick to essentials this week.
                        </p>
                      )}
                    {(intel.daysUntilSalary ?? 0) > 7 && (
                      <p>
                        Salary arrives in {intel.daysUntilSalary} days. You&apos;re in good shape
                        with {formatInr(intel.dailyBudget ?? 0)}/day to spend freely.
                      </p>
                    )}
                  </div>
                </div>
              )}

              {intel.zone === "POST_SALARY" && (
                <div className="card salary-intel-card post-salary">
                  <span className="salary-active-badge">✓ Salary month active</span>
                  <p className="salary-daily-hero">{formatInr(intel.dailyBudget ?? 0)}/day</p>
                  <p className="chart-subtitle">
                    your daily spending budget for {intel.monthLabel}
                  </p>
                </div>
              )}

              {showAllocation && intel.allocationPlan && (
                <div className="card allocation-plan-card">
                  <div className="allocation-plan-header">
                    <div>
                      <h2>Monthly Allocation Plan 🎯</h2>
                      <p className="chart-subtitle">
                        Here&apos;s how to distribute this month&apos;s{" "}
                        {formatInr(intel.allocationPlan.salaryAmount)}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="advisor-card-dismiss"
                      aria-label="Dismiss allocation plan"
                      onClick={dismissAllocation}
                    >
                      ×
                    </button>
                  </div>
                  <ul className="allocation-breakdown">
                    {intel.allocationPlan.breakdown.map(item => (
                      <li key={item.label} className={`allocation-row kind-${item.kind || "fixed"}`}>
                        <span className="allocation-label">{item.label}</span>
                        <div className="allocation-bar-track">
                          <div
                            className="allocation-bar-fill"
                            style={{
                              width: `${Math.min(
                                100,
                                intel.allocationPlan!.salaryAmount > 0
                                  ? (item.amount / intel.allocationPlan!.salaryAmount) * 100
                                  : 0
                              )}%`
                            }}
                          />
                        </div>
                        <span className="allocation-amount">{formatInr(item.amount)}</span>
                      </li>
                    ))}
                  </ul>
                  <div className="allocation-totals">
                    <p>
                      <strong>Total allocated:</strong> {formatInr(allocationTotal)}
                    </p>
                    {Math.abs(allocationRemaining) >= 1 && (
                      <p>
                        <strong>Remaining unallocated:</strong>{" "}
                        {formatInr(allocationRemaining)}
                      </p>
                    )}
                  </div>
                </div>
              )}

              <div className="card daily-budget-tracker">
                <h2>Today&apos;s spending so far</h2>
                <div className="daily-budget-stats">
                  <span>Spent today: {formatInr(todaySpent)}</span>
                  <span>Daily budget: {formatInr(dailyBudget)}</span>
                  <span className={remainingToday >= 0 ? "text-green" : "text-red"}>
                    Remaining today: {formatInr(remainingToday)}
                  </span>
                </div>
                <div className="daily-budget-progress-track">
                  <div
                    className={`daily-budget-progress-fill ${
                      dailyPct > 100 ? "over" : dailyPct >= 70 ? "warn" : "ok"
                    }`}
                    style={{ width: `${Math.min(100, dailyPct)}%` }}
                  />
                </div>
                <p className="daily-budget-message">
                  {dailyPct <= 100 ? (
                    <>You&apos;re within today&apos;s budget ✓</>
                  ) : (
                    <>
                      You&apos;ve exceeded today&apos;s budget by{" "}
                      {formatInr(Math.abs(remainingToday))}. Consider spending less tomorrow.
                    </>
                  )}
                </p>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
