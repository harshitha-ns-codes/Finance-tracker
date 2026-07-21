import { FormEvent, ReactNode, useEffect, useState } from "react";
import { CategoryRegretStats, PurchaseSimulation, getRegretStats, simulatePurchase } from "../api";
import { BUDGET_CATEGORIES } from "../categories";

function fmt(n: number | undefined): string {
  if (n == null || Number.isNaN(n)) return "₹0";
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function pct(n: number | undefined): string {
  if (n == null || Number.isNaN(n)) return "0%";
  return `${Math.round(n)}%`;
}

function verdictMeta(verdict: string): { title: string; className: string } {
  if (verdict === "GO_AHEAD") {
    return { title: "You can afford this", className: "verdict-go" };
  }
  if (verdict === "CONSIDER") {
    return { title: "Possible but risky", className: "verdict-consider" };
  }
  return { title: "Not recommended now", className: "verdict-avoid" };
}

type Props = {
  defaultCategory?: string;
  profileBalanceHint?: boolean;
  onRunScenarios?: () => void;
  simulatingScenarios?: boolean;
  scenariosSlot?: ReactNode;
  onSimulated?: (input: { price: number; category: string; itemName: string }) => void;
};

export function WhatIfSimulatorPanel({
  defaultCategory = "Shopping",
  profileBalanceHint = false,
  onRunScenarios,
  simulatingScenarios = false,
  scenariosSlot,
  onSimulated
}: Props) {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [itemName, setItemName] = useState("Shoes");
  const [price, setPrice] = useState(4500);
  const [category, setCategory] = useState(defaultCategory);
  const [priority, setPriority] = useState<"NEED" | "WANT" | "INVESTMENT">("WANT");
  const [paymentDate, setPaymentDate] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<PurchaseSimulation | null>(null);
  const [animateScore, setAnimateScore] = useState(false);
  const [categoryRegret, setCategoryRegret] = useState<CategoryRegretStats | null>(null);

  useEffect(() => {
    if (step === 2 && result) {
      setAnimateScore(false);
      const t = window.setTimeout(() => setAnimateScore(true), 40);
      return () => window.clearTimeout(t);
    }
  }, [step, result]);

  const handleSimulate = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await simulatePurchase({
        itemName: itemName.trim() || "Purchase",
        price: Number(price) || 0,
        category,
        priority,
        paymentDate: paymentDate || undefined
      });
      setResult(data);
      setStep(2);
      try {
        const regretStats = await getRegretStats();
        const catStats = regretStats.regretByCategory.find(
          c => c.category.toLowerCase() === category.toLowerCase()
        );
        setCategoryRegret(catStats ?? null);
      } catch {
        setCategoryRegret(null);
      }
      onSimulated?.({
        price: Number(price) || 0,
        category,
        itemName: itemName.trim() || "Purchase"
      });
    } catch (err: any) {
      setError(err.message || "Simulation failed");
    } finally {
      setLoading(false);
    }
  };

  const balanceBefore =
    result != null
      ? Number(result.immediateImpact.newBalance) - Number(result.immediateImpact.balanceChange)
      : 0;
  const balanceAfter = result ? Number(result.immediateImpact.newBalance) : 0;
  const budget = result?.budgetImpact;
  const health = result?.healthScoreImpact;
  const savings = result?.savingsImpact;
  const verdict = result ? verdictMeta(result.verdict) : null;

  const budgetBeforePct =
    budget && Number(budget.categoryBudget) > 0
      ? (Number(budget.categorySpent) / Number(budget.categoryBudget)) * 100
      : 0;
  const budgetAfterPct =
    budget && Number(budget.categoryBudget) > 0
      ? (Number(budget.afterPurchase) / Number(budget.categoryBudget)) * 100
      : 0;

  return (
    <div className="whatif-panel">
      <div className="whatif-steps">
        <button
          type="button"
          className={`whatif-step ${step === 1 ? "active" : ""}`}
          onClick={() => setStep(1)}
        >
          1 · Input
        </button>
        <button
          type="button"
          className={`whatif-step ${step === 2 ? "active" : ""}`}
          onClick={() => result && setStep(2)}
          disabled={!result}
        >
          2 · Results
        </button>
        <button
          type="button"
          className={`whatif-step ${step === 3 ? "active" : ""}`}
          onClick={() => setStep(3)}
        >
          3 · Scenarios
        </button>
      </div>

      {step === 1 && (
        <>
          <p className="chart-subtitle">
            Simulate a purchase against your balance, category budget, health score, and savings.
            {profileBalanceHint && (
              <>
                {" "}
                Set <strong>current balance</strong> under Advisor inputs for better accuracy.
              </>
            )}
          </p>
          <form className="form decision-form" onSubmit={handleSimulate}>
            <label>
              Item name
              <input
                value={itemName}
                onChange={e => setItemName(e.target.value)}
                placeholder="e.g. Running shoes"
                required
              />
            </label>
            <label>
              Category
              <select value={category} onChange={e => setCategory(e.target.value)}>
                {BUDGET_CATEGORIES.map(c => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Price (₹)
              <input
                type="number"
                min="0"
                step="0.01"
                value={price}
                onChange={e => setPrice(Number(e.target.value) || 0)}
                required
              />
            </label>
            <label>
              Priority
              <select
                value={priority}
                onChange={e =>
                  setPriority(e.target.value as "NEED" | "WANT" | "INVESTMENT")
                }
              >
                <option value="NEED">Need</option>
                <option value="WANT">Want</option>
                <option value="INVESTMENT">Investment</option>
              </select>
            </label>
            <label>
              Payment date (optional)
              <input
                type="date"
                value={paymentDate}
                onChange={e => setPaymentDate(e.target.value)}
              />
            </label>
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? "Simulating…" : "Simulate"}
            </button>
          </form>
          {error && <div className="error" style={{ marginTop: "0.75rem" }}>{error}</div>}
        </>
      )}

      {step === 2 && result && verdict && (
        <div className="whatif-results">
          <div className="whatif-impact-grid">
            <div className="whatif-impact-card">
              <span className="whatif-card-label">Balance impact</span>
              <strong className="whatif-card-value">
                {fmt(balanceBefore)} → {fmt(balanceAfter)}
              </strong>
              <p className="chart-subtitle">
                {pct(result.immediateImpact.percentOfBalance)} of spendable balance
              </p>
              <div className="whatif-mini-bars">
                <div className="whatif-mini-bar">
                  <span>Before</span>
                  <div>
                    <i
                      style={{
                        width: `${Math.min(
                          100,
                          Math.max(8, (Math.max(balanceBefore, 0) /
                            Math.max(Math.abs(balanceBefore), Math.abs(balanceAfter), 1)) *
                            100)
                        )}%`
                      }}
                    />
                  </div>
                </div>
                <div className="whatif-mini-bar">
                  <span>After</span>
                  <div>
                    <i
                      className={balanceAfter < 0 ? "bad" : ""}
                      style={{
                        width: `${Math.min(
                          100,
                          Math.max(
                            8,
                            (Math.max(balanceAfter, 0) /
                              Math.max(Math.abs(balanceBefore), Math.abs(balanceAfter), 1)) *
                              100
                          )
                        )}%`
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>

            <div
              className={`whatif-impact-card ${
                budget?.willExceedBudget ? "tone-bad" : "tone-good"
              }`}
            >
              <span className="whatif-card-label">Budget impact</span>
              <strong className="whatif-card-value">
                {category}: {fmt(budget?.categorySpent)}/{fmt(budget?.categoryBudget)} →{" "}
                {fmt(budget?.afterPurchase)}/{fmt(budget?.categoryBudget)}{" "}
                ({Math.round(budgetAfterPct)}%)
              </strong>
              <p className="chart-subtitle">
                {budget?.willExceedBudget
                  ? `Exceeds budget by ${fmt(budget.exceedBy)}`
                  : `Within budget (${Math.round(budgetBeforePct)}% → ${Math.round(budgetAfterPct)}%)`}
              </p>
            </div>

            <div className="whatif-impact-card">
              <span className="whatif-card-label">Health score impact</span>
              <strong className={`whatif-card-value ${animateScore ? "score-animate" : ""}`}>
                {health?.currentScore} → {health?.projectedScore}{" "}
                <span className={Number(health?.change) < 0 ? "text-red" : "text-green"}>
                  ({Number(health?.change) > 0 ? "+" : ""}
                  {health?.change} points)
                </span>
              </strong>
              <p className="chart-subtitle">{health?.reason}</p>
            </div>

            <div className="whatif-impact-card">
              <span className="whatif-card-label">Savings impact</span>
              <strong className="whatif-card-value">
                Savings rate: {pct(savings?.currentSavingsRate)} →{" "}
                {pct(savings?.afterSavingsRate)}
              </strong>
              <p className="chart-subtitle">
                Monthly savings {fmt(savings?.currentMonthlySavings)} →{" "}
                {fmt(savings?.afterPurchaseSavings)} (
                {Number(savings?.savingsRateChange) > 0 ? "+" : ""}
                {savings?.savingsRateChange ?? 0} pts)
              </p>
            </div>
          </div>

          <div className={`whatif-verdict ${verdict.className}`}>
            <div className="whatif-verdict-top">
              <strong>{result.verdict.replace("_", " ")}</strong>
              <span>{verdict.title}</span>
            </div>
            <p>{result.verdictReason}</p>
            <p className="chart-subtitle">
              Affordability score: {result.affordabilityScore}/100
              {result.canAfford ? " · Can afford" : " · Stretch / risk"}
            </p>
          </div>

          {categoryRegret &&
            categoryRegret.regretRate > 50 &&
            categoryRegret.reviewed >= 1 && (
              <div className="regret-simulator-warning">
                ⚠️ You&apos;ve regretted {Math.round(categoryRegret.regretRate)}% of past{" "}
                {category} purchases. Consider waiting 24 hours before deciding.
              </div>
            )}

          {result.alternatives?.length > 0 && (
            <div className="whatif-alts">
              <h3>What you could do instead</h3>
              <ul>
                {result.alternatives.map((a, i) => (
                  <li key={i}>{a}</li>
                ))}
              </ul>
            </div>
          )}

          <div className="whatif-actions">
            <button type="button" className="btn-secondary" onClick={() => setStep(1)}>
              Edit inputs
            </button>
            <button type="button" className="btn-primary" onClick={() => setStep(3)}>
              What-if scenarios
            </button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="whatif-scenarios">
          <p className="chart-subtitle">
            Compare alternate paths — wait for salary, cut category spend, or a raise.
          </p>
          {onRunScenarios && (
            <button
              type="button"
              className="btn-primary"
              disabled={simulatingScenarios}
              onClick={() => onRunScenarios()}
            >
              {simulatingScenarios ? "Running…" : "Run what-if scenarios"}
            </button>
          )}
          {scenariosSlot}
          <div className="whatif-actions" style={{ marginTop: "1rem" }}>
            <button type="button" className="btn-secondary" onClick={() => setStep(2)} disabled={!result}>
              Back to results
            </button>
            <button type="button" className="btn-secondary" onClick={() => setStep(1)}>
              New simulation
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
