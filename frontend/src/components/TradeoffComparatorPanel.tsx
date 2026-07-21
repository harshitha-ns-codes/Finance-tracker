import { FormEvent, useState } from "react";
import {
  TradeoffComparison,
  TradeoffOptionResult,
  compareTradeoff
} from "../api";

function fmt(n: number | undefined): string {
  if (n == null || Number.isNaN(n)) return "₹0";
  const abs = Math.abs(Math.round(n));
  const sign = n < 0 ? "-" : n > 0 ? "+" : "";
  return `${sign}₹${abs.toLocaleString("en-IN")}`;
}

function scoreLabel(n: number): string {
  return `${n > 0 ? "+" : ""}${n}`;
}

type OptionDraft = {
  name: string;
  amount: number;
  type: "PURCHASE" | "SAVING";
};

function OptionResultColumn({
  label,
  option,
  isWinner
}: {
  label: string;
  option: TradeoffOptionResult;
  isWinner: boolean;
}) {
  return (
    <div className={`tradeoff-result-col ${isWinner ? "winner" : ""}`}>
      {isWinner && <span className="tradeoff-winner-badge">Recommended</span>}
      <h3>
        {label}: {option.name}
      </h3>
      <div className="tradeoff-stat-grid">
        <div>
          <span>Balance impact</span>
          <strong className={option.immediateBalanceImpact < 0 ? "text-red" : "text-green"}>
            {fmt(option.immediateBalanceImpact)}
          </strong>
        </div>
        <div>
          <span>Monthly impact</span>
          <strong className={option.monthlyImpact < 0 ? "text-red" : "text-green"}>
            {fmt(option.monthlyImpact)}
          </strong>
        </div>
        <div>
          <span>Health score</span>
          <strong className={option.healthScoreImpact < 0 ? "text-red" : "text-green"}>
            {scoreLabel(option.healthScoreImpact)} pts
          </strong>
        </div>
        <div>
          <span>Timeline</span>
          <strong>{option.timeToRecover}</strong>
        </div>
      </div>
      <div className="tradeoff-lists">
        <div>
          <h4>Pros</h4>
          <ul className="tradeoff-pros">
            {(option.pros || []).map((p, i) => (
              <li key={i}>{p}</li>
            ))}
          </ul>
        </div>
        <div>
          <h4>Cons</h4>
          <ul className="tradeoff-cons">
            {(option.cons || []).map((c, i) => (
              <li key={i}>{c}</li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

export function TradeoffComparatorPanel() {
  const [optionA, setOptionA] = useState<OptionDraft>({
    name: "Buy Shoes",
    amount: 4500,
    type: "PURCHASE"
  });
  const [optionB, setOptionB] = useState<OptionDraft>({
    name: "Save for Emergency Fund",
    amount: 4500,
    type: "SAVING"
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TradeoffComparison | null>(null);

  const handleCompare = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await compareTradeoff({
        option1: optionA,
        option2: optionB
      });
      setResult(data);
    } catch (err: any) {
      setError(err.message || "Comparison failed");
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tradeoff-panel">
      <p className="chart-subtitle">
        Compare a purchase against saving (or any two money moves) to see which helps your finances more.
      </p>

      <form className="tradeoff-form" onSubmit={handleCompare}>
        <div className="tradeoff-input-grid">
          <div className="card tradeoff-input-card">
            <h3>Option A</h3>
            <label>
              Name
              <input
                value={optionA.name}
                onChange={e => setOptionA(prev => ({ ...prev, name: e.target.value }))}
                required
              />
            </label>
            <label>
              Amount (₹)
              <input
                type="number"
                min="0"
                step="0.01"
                value={optionA.amount}
                onChange={e =>
                  setOptionA(prev => ({ ...prev, amount: Number(e.target.value) || 0 }))
                }
                required
              />
            </label>
            <label>
              Type
              <select
                value={optionA.type}
                onChange={e =>
                  setOptionA(prev => ({
                    ...prev,
                    type: e.target.value as "PURCHASE" | "SAVING"
                  }))
                }
              >
                <option value="PURCHASE">Purchase</option>
                <option value="SAVING">Saving</option>
              </select>
            </label>
          </div>

          <div className="card tradeoff-input-card">
            <h3>Option B</h3>
            <label>
              Name
              <input
                value={optionB.name}
                onChange={e => setOptionB(prev => ({ ...prev, name: e.target.value }))}
                required
              />
            </label>
            <label>
              Amount (₹)
              <input
                type="number"
                min="0"
                step="0.01"
                value={optionB.amount}
                onChange={e =>
                  setOptionB(prev => ({ ...prev, amount: Number(e.target.value) || 0 }))
                }
                required
              />
            </label>
            <label>
              Type
              <select
                value={optionB.type}
                onChange={e =>
                  setOptionB(prev => ({
                    ...prev,
                    type: e.target.value as "PURCHASE" | "SAVING"
                  }))
                }
              >
                <option value="PURCHASE">Purchase</option>
                <option value="SAVING">Saving</option>
              </select>
            </label>
          </div>
        </div>

        <div className="tradeoff-actions">
          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? "Comparing…" : "Compare"}
          </button>
        </div>
      </form>

      {error && <div className="error">{error}</div>}

      {result && (
        <div className="tradeoff-results">
          <div className="tradeoff-result-grid">
            <OptionResultColumn
              label="Option A"
              option={result.option1}
              isWinner={result.recommendation === "option1"}
            />
            <OptionResultColumn
              label="Option B"
              option={result.option2}
              isWinner={result.recommendation === "option2"}
            />
          </div>
          <div className="tradeoff-recommendation">
            <span className="narrative-label">Recommendation</span>
            <p>{result.recommendationReason}</p>
          </div>
        </div>
      )}
    </div>
  );
}
