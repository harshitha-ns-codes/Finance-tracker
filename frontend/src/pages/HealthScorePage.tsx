import { FormEvent, useEffect, useState } from "react";
import {
  CategoryScore,
  FinancialAdvisory,
  FinancialProfile,
  HealthNarrative,
  HealthScore,
  SubscriptionItem,
  createSubscription,
  getFinancialAdvisory,
  getFinancialProfile,
  getHealthNarrative,
  getHealthScore,
  listSubscriptions,
  runSimulations,
  saveFinancialProfile
} from "../api";
import { TradeoffComparatorPanel } from "../components/TradeoffComparatorPanel";
import { WhatIfSimulatorPanel } from "../components/WhatIfSimulatorPanel";
import { BUDGET_CATEGORIES } from "../categories";

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function riskClass(level: string): string {
  return `risk-${level.toLowerCase()}`;
}

function fmt(n: number | undefined): string {
  if (n == null) return "₹0";
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

export function HealthScorePage() {
  const [month, setMonth] = useState(currentMonth);
  const [score, setScore] = useState<HealthScore | null>(null);
  const [advisory, setAdvisory] = useState<FinancialAdvisory | null>(null);
  const [narrative, setNarrative] = useState<HealthNarrative | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openCats, setOpenCats] = useState<Record<string, boolean>>({});
  const [showPrediction, setShowPrediction] = useState(true);
  const [showPositives, setShowPositives] = useState(true);
  const [showNegatives, setShowNegatives] = useState(true);
  const [showInputs, setShowInputs] = useState(false);

  const [profile, setProfile] = useState<FinancialProfile | null>(null);
  const [subs, setSubs] = useState<SubscriptionItem[]>([]);

  const [scenarioPrice, setScenarioPrice] = useState(4500);
  const [scenarioCategory, setScenarioCategory] = useState<string>(BUDGET_CATEGORIES[2]);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [simulating, setSimulating] = useState(false);

  const loadAll = async () => {
    setLoading(true);
    setError(null);
    try {
      const healthData = await getHealthScore(month);
      setScore(healthData);
      try {
        const advisoryData = await getFinancialAdvisory(month);
        setAdvisory(advisoryData);
      } catch {
        setAdvisory(null);
      }
      try {
        const narrativeData = await getHealthNarrative(month);
        setNarrative(narrativeData);
      } catch {
        setNarrative(null);
      }
    } catch (err: any) {
      setError(err.message || "Failed to load advisor");
    } finally {
      setLoading(false);
    }
  };

  const loadInputs = async () => {
    try {
      const [p, s] = await Promise.all([getFinancialProfile(), listSubscriptions()]);
      setProfile(p);
      setSubs(s);
    } catch {
      // optional
    }
  };

  useEffect(() => {
    void loadAll();
    void loadInputs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [month]);

  const toggleCat = (name: string) => {
    setOpenCats(prev => ({ ...prev, [name]: !prev[name] }));
  };

  const handleSimulate = async () => {
    setSimulating(true);
    setDecisionError(null);
    try {
      const result = await runSimulations(
        [
          {
            scenarioName: "Wait until salary",
            postponePurchaseDays: 7,
            purchaseAmount: Number(scenarioPrice) || 0,
            purchaseCategory: scenarioCategory
          },
          {
            scenarioName: `Cut ${scenarioCategory} by ₹2000`,
            categorySpendAdjustments: { [scenarioCategory]: -2000 },
            purchaseAmount: Number(scenarioPrice) || 0,
            purchaseCategory: scenarioCategory
          },
          {
            scenarioName: "Salary increase ₹5000",
            salaryIncrease: 5000,
            purchaseAmount: Number(scenarioPrice) || 0,
            purchaseCategory: scenarioCategory
          }
        ],
        month
      );
      setAdvisory(prev => (prev ? { ...prev, simulations: result.simulations ?? [] } : result));
      if (!result.simulations?.length) {
        setDecisionError("No simulation results. Restart the backend to load the advisory engine.");
      }
    } catch (err: any) {
      setDecisionError(err.message || "Simulation failed. Restart backend if this persists.");
    } finally {
      setSimulating(false);
    }
  };

  const saveProfile = async (e: FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    try {
      const saved = await saveFinancialProfile({
        emergencyFundBalance: Number(profile.emergencyFundBalance) || 0,
        monthlyDebtPayments: Number(profile.monthlyDebtPayments) || 0,
        totalDebtOutstanding: Number(profile.totalDebtOutstanding) || 0,
        currentBalance: Number(profile.currentBalance) || 0,
        salaryDayOfMonth: Number(profile.salaryDayOfMonth) || 1
      });
      setProfile(saved);
      await loadAll();
    } catch (err: any) {
      setError(err.message || "Failed to save profile");
    }
  };

  const addSub = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    try {
      await createSubscription({
        name: String(fd.get("name") || "Subscription"),
        monthlyAmount: Number(fd.get("monthlyAmount")) || 0,
        unused: fd.get("unused") === "on",
        duplicate: fd.get("duplicate") === "on"
      });
      e.currentTarget.reset();
      await loadInputs();
      await loadAll();
    } catch (err: any) {
      setError(err.message || "Failed to add subscription");
    }
  };

  const pct = score ? Math.max(0, Math.min(100, score.score)) : 0;
  const prediction = advisory?.prediction;

  return (
    <div className="page health-page">
      <div className="budget-page-header">
        <div>
          <h1>Advisor</h1>
          <p className="chart-subtitle">
            Narrative, health score, predictions, and decision tools — powered by the same Plan
            spent totals as Home.
          </p>
        </div>
        <label className="month-select">
          Month
          <input type="month" value={month} onChange={e => setMonth(e.target.value)} />
        </label>
      </div>

      {error && <div className="error">{error}</div>}

      {loading || !score ? (
        <div className="card">Loading your advisor…</div>
      ) : (
        <>
          {narrative?.narrative && (
            <div className={`narrative-card tone-${narrative.tone || "positive"}`}>
              <span className="narrative-label">Your month so far</span>
              <p className="narrative-text">{narrative.narrative}</p>
            </div>
          )}

          <div className="card health-hero">
            <div className="health-hero-main">
              <div className="health-score-ring" style={{ ["--pct" as string]: `${pct}` }}>
                <div className="health-score-inner">
                  <span className="health-score-value">{score.score}</span>
                  <span className="health-score-max">/100</span>
                </div>
              </div>
              <div>
                <p className="health-rating">{score.rating}</p>
                {score.monthDelta != null && (
                  <p className={score.monthDelta >= 0 ? "health-delta up" : "health-delta down"}>
                    {score.monthDelta >= 0 ? "▲" : "▼"}{" "}
                    {score.monthDelta >= 0 ? "+" : ""}
                    {score.monthDelta} this month
                  </p>
                )}
                <p className="chart-subtitle">
                  Built from budget, savings, emergency fund, bills, consistency, goals, debt &
                  subscriptions.
                </p>
              </div>
            </div>
            <div className="health-progress-track">
              <div className="health-progress-fill" style={{ width: `${pct}%` }} />
            </div>
          </div>

          <div className="card">
            <h2>Score breakdown</h2>
            <p className="chart-subtitle">Expand any category to see the exact explanation.</p>
            <div className="breakdown-list">
              {score.breakdown.map((cat: CategoryScore) => {
                const open = !!openCats[cat.category];
                const ratio = cat.max ? (cat.score / cat.max) * 100 : 0;
                return (
                  <div key={cat.category} className="breakdown-item">
                    <button
                      type="button"
                      className="breakdown-header"
                      onClick={() => toggleCat(cat.category)}
                    >
                      <div>
                        <strong>{cat.category}</strong>
                        <span>
                          {cat.score}/{cat.max}
                        </span>
                      </div>
                      <span>{open ? "−" : "+"}</span>
                    </button>
                    <div className="mini-progress">
                      <div style={{ width: `${ratio}%` }} />
                    </div>
                    {open && (
                      <div className="breakdown-body">
                        <p>{cat.explanation}</p>
                        {cat.details && cat.details.length > 0 && (
                          <ul>
                            {cat.details.map((d, i) => (
                              <li key={i}>{d}</li>
                            ))}
                          </ul>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {prediction && (
            <div className="card">
              <button
                type="button"
                className="section-toggle"
                onClick={() => setShowPrediction(v => !v)}
              >
                <h2>Budget prediction</h2>
                <span>{showPrediction ? "−" : "+"}</span>
              </button>
              {showPrediction && (
                <>
                  <div className="prediction-summary">
                    <div className="prediction-stat">
                      <span className="chart-subtitle">Expected spend</span>
                      <strong>{fmt(prediction.expectedMonthlySpend)}</strong>
                    </div>
                    <div className="prediction-stat">
                      <span className="chart-subtitle">Budget</span>
                      <strong>{fmt(prediction.totalBudget)}</strong>
                    </div>
                    <div className="prediction-stat">
                      <span className="chart-subtitle">Expected savings</span>
                      <strong>{fmt(prediction.expectedSavings)}</strong>
                    </div>
                    <div className="prediction-stat">
                      <span className="chart-subtitle">Overspend risk</span>
                      <strong className={riskClass(prediction.riskLevel)}>
                        {prediction.riskLevel} ({Math.round(prediction.overspendProbability)}%)
                      </strong>
                    </div>
                  </div>
                  {prediction.categories.length > 0 && (
                    <table className="prediction-table">
                      <thead>
                        <tr>
                          <th>Category</th>
                          <th>Current</th>
                          <th>Predicted</th>
                          <th>Budget</th>
                          <th>Risk</th>
                        </tr>
                      </thead>
                      <tbody>
                        {prediction.categories.map(c => (
                          <tr key={c.category}>
                            <td>{c.category}</td>
                            <td>{fmt(c.currentSpend)}</td>
                            <td>{fmt(c.predictedSpend)}</td>
                            <td>{fmt(c.budget)}</td>
                            <td className={riskClass(c.riskLevel)}>
                              {c.riskLevel} ({Math.round(c.riskScorePercent)}%)
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </>
              )}
            </div>
          )}

          <div className="card decision-card">
            <h2>What-If Simulator</h2>
            <WhatIfSimulatorPanel
              defaultCategory={scenarioCategory}
              profileBalanceHint={(profile?.currentBalance ?? 0) === 0}
              simulatingScenarios={simulating}
              onSimulated={({ price, category }) => {
                setScenarioPrice(price);
                setScenarioCategory(category);
              }}
              onRunScenarios={() => void handleSimulate()}
              scenariosSlot={
                <>
                  {decisionError && (
                    <div className="error" style={{ marginTop: "0.75rem" }}>
                      {decisionError}
                    </div>
                  )}
                  {advisory?.simulations && advisory.simulations.length > 0 && (
                    <div style={{ marginTop: "1rem" }}>
                      <h3>Scenario comparison</h3>
                      <table className="prediction-table">
                        <thead>
                          <tr>
                            <th>Scenario</th>
                            <th>Health</th>
                            <th>Spend</th>
                            <th>Decision</th>
                          </tr>
                        </thead>
                        <tbody>
                          {advisory.simulations.map((s, i) => (
                            <tr key={i}>
                              <td>{s.scenarioName}</td>
                              <td>{s.healthScore ?? "—"}</td>
                              <td>
                                {s.expectedMonthlySpend != null
                                  ? fmt(s.expectedMonthlySpend)
                                  : "—"}
                              </td>
                              <td>
                                {s.purchaseDecision ?? "—"}
                                {s.purchaseAffordabilityScore != null &&
                                  ` (${s.purchaseAffordabilityScore}/100)`}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              }
            />
          </div>

          <div className="card tradeoff-card">
            <h2>Trade-off Comparator</h2>
            <TradeoffComparatorPanel />
          </div>

          <div className="card">
            <button
              type="button"
              className="section-toggle"
              onClick={() => setShowPositives(v => !v)}
            >
              <h2>What you&apos;re doing well</h2>
              <span>{showPositives ? "−" : "+"}</span>
            </button>
            {showPositives && (
              <ul className="insight-list positive">
                {score.positives.map((p, i) => (
                  <li key={i}>{p}</li>
                ))}
              </ul>
            )}
          </div>

          <div className="card">
            <button
              type="button"
              className="section-toggle"
              onClick={() => setShowNegatives(v => !v)}
            >
              <h2>Areas to improve</h2>
              <span>{showNegatives ? "−" : "+"}</span>
            </button>
            {showNegatives && (
              <ul className="insight-list negative">
                {score.negatives.length === 0 ? (
                  <li>No major weak spots detected for this month.</li>
                ) : (
                  score.negatives.map((n, i) => <li key={i}>{n}</li>)
                )}
              </ul>
            )}
          </div>

          <div className="card">
            <button
              type="button"
              className="section-toggle"
              onClick={() => setShowInputs(v => !v)}
            >
              <h2>Profile &amp; subscriptions</h2>
              <span>{showInputs ? "−" : "+"}</span>
            </button>
            {showInputs && (
              <div className="health-inputs">
                <form className="form" onSubmit={saveProfile}>
                  <h3>Profile</h3>
                  <label>
                    Current balance (₹)
                    <input
                      type="number"
                      value={profile?.currentBalance ?? 0}
                      onChange={e =>
                        setProfile(p => ({
                          ...(p || {
                            emergencyFundBalance: 0,
                            monthlyDebtPayments: 0,
                            totalDebtOutstanding: 0,
                            currentBalance: 0,
                            salaryDayOfMonth: 1
                          }),
                          currentBalance: Number(e.target.value) || 0
                        }))
                      }
                    />
                  </label>
                  <label>
                    Emergency fund balance (₹)
                    <input
                      type="number"
                      value={profile?.emergencyFundBalance ?? 0}
                      onChange={e =>
                        setProfile(p => ({
                          ...(p || {
                            emergencyFundBalance: 0,
                            monthlyDebtPayments: 0,
                            totalDebtOutstanding: 0,
                            currentBalance: 0,
                            salaryDayOfMonth: 1
                          }),
                          emergencyFundBalance: Number(e.target.value) || 0
                        }))
                      }
                    />
                  </label>
                  <label>
                    Salary day (1–31)
                    <input
                      type="number"
                      min={1}
                      max={31}
                      value={profile?.salaryDayOfMonth ?? 1}
                      onChange={e =>
                        setProfile(p => ({
                          ...(p || {
                            emergencyFundBalance: 0,
                            monthlyDebtPayments: 0,
                            totalDebtOutstanding: 0,
                            currentBalance: 0,
                            salaryDayOfMonth: 1
                          }),
                          salaryDayOfMonth: Number(e.target.value) || 1
                        }))
                      }
                    />
                  </label>
                  <label>
                    Monthly debt payments (₹)
                    <input
                      type="number"
                      value={profile?.monthlyDebtPayments ?? 0}
                      onChange={e =>
                        setProfile(p => ({
                          ...(p || {
                            emergencyFundBalance: 0,
                            monthlyDebtPayments: 0,
                            totalDebtOutstanding: 0,
                            currentBalance: 0,
                            salaryDayOfMonth: 1
                          }),
                          monthlyDebtPayments: Number(e.target.value) || 0
                        }))
                      }
                    />
                  </label>
                  <button className="btn-secondary" type="submit">
                    Save profile
                  </button>
                </form>

                <form className="form" onSubmit={addSub}>
                  <h3>Add subscription</h3>
                  <label>
                    Name
                    <input name="name" required placeholder="Netflix" />
                  </label>
                  <label>
                    Monthly (₹)
                    <input name="monthlyAmount" type="number" required />
                  </label>
                  <label className="check-row">
                    <input name="unused" type="checkbox" /> Unused
                  </label>
                  <label className="check-row">
                    <input name="duplicate" type="checkbox" /> Duplicate
                  </label>
                  <button className="btn-secondary" type="submit">
                    Add subscription
                  </button>
                </form>
                {subs.length > 0 && (
                  <ul className="mini-list">
                    {subs.map(s => (
                      <li key={s.id}>
                        {s.name}: ₹{s.monthlyAmount}/mo
                        {s.unused ? " · unused" : ""}
                        {s.duplicate ? " · duplicate" : ""}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
