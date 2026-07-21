import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Rule502030Analysis,
  classifyTransaction,
  get502030Analysis
} from "../api";
import { NetWorthSection } from "../components/NetWorthSection";

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function formatInr(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function formatTopCategories(items: { category: string; amount: number }[] | undefined): string {
  if (!items?.length) return "";
  return items.map(c => `${c.category} ${formatInr(c.amount)}`).join(", ");
}

function statusBadge(status: string, diff: number) {
  if (status === "ON_TRACK") {
    return <span className="rule-badge on-track">On track ✓</span>;
  }
  if (status === "OVER") {
    return <span className="rule-badge over">Over by {Math.abs(diff).toFixed(0)}%</span>;
  }
  return <span className="rule-badge under">Under by {Math.abs(diff).toFixed(0)}%</span>;
}

type SegmentProps = {
  label: string;
  percent: number;
  amount: number;
  className: string;
};

function BarSegment({ label, percent, amount, className }: SegmentProps) {
  if (percent <= 0) return null;
  return (
    <div
      className={`rule-bar-segment ${className}`}
      style={{ width: `${Math.min(100, percent)}%` }}
      title={`${label}: ${formatInr(amount)} (${percent.toFixed(1)}%)`}
    />
  );
}

export function ProfilePage() {
  const [month, setMonth] = useState(currentMonth);
  const [data, setData] = useState<Rule502030Analysis | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unclassifiedOpen, setUnclassifiedOpen] = useState(true);
  const [classifyingId, setClassifyingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await get502030Analysis(month));
    } catch (err: any) {
      const msg = err.message || "Failed to load 50/30/20 analysis";
      if (/unauthorized|log in again|session expired|invalid or expired token/i.test(msg)) {
        setError("Session expired. Log out and sign in again, then reopen Profile.");
      } else {
        setError(msg);
      }
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [month]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleClassify = async (id: number, needType: "NEED" | "WANT" | "SAVING") => {
    setClassifyingId(id);
    try {
      await classifyTransaction(id, needType);
      await load();
    } catch (err: any) {
      const msg = err.message || "Failed to classify transaction";
      if (/unauthorized|log in again|session expired/i.test(msg)) {
        setError("Session expired. Log out and sign in again.");
      } else {
        setError(msg);
      }
    } finally {
      setClassifyingId(null);
    }
  };

  const remainingUnclassified = data?.unclassifiedTransactions.length ?? 0;

  const barSegments = useMemo(() => {
    if (!data) return { needs: 0, wants: 0, savings: 0 };
    const needs = Math.max(0, data.needs.percent);
    const wants = Math.max(0, data.wants.percent);
    const savings = Math.max(0, data.savings.percent);
    const total = needs + wants + savings || 1;
    return {
      needs: (needs / total) * 100,
      wants: (wants / total) * 100,
      savings: (savings / total) * 100
    };
  }, [data]);

  return (
    <div className="page profile-page">
      <div className="budget-page-header">
        <div>
          <h1>Profile</h1>
          <p className="chart-subtitle">Your financial breakdown and settings</p>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      <section className="card rule502030-section">
        <div className="rule502030-header">
          <div>
            <h2>50/30/20 Analysis</h2>
            <p className="chart-subtitle">How your spending compares to the ideal rule</p>
          </div>
          <label className="month-select">
            Month
            <input type="month" value={month} onChange={e => setMonth(e.target.value)} />
          </label>
        </div>

        {loading ? (
          <p className="chart-subtitle">Loading analysis…</p>
        ) : !data ? (
          <p className="chart-subtitle">No analysis available for this month.</p>
        ) : (
          <>
            <div className="rule502030-columns">
              <div className="rule502030-col needs">
                <span className="rule502030-label">NEEDS</span>
                <span className="rule502030-sub">Rent, food, transport, bills</span>
                <span className="rule502030-pct">{data.needs.percent.toFixed(0)}%</span>
                <span className="rule502030-ideal">Ideal: 50%</span>
                {statusBadge(data.needs.status, data.needs.diff)}
              </div>
              <div className="rule502030-col wants">
                <span className="rule502030-label">WANTS</span>
                <span className="rule502030-sub">Shopping, dining, entertainment</span>
                <span className="rule502030-pct">{data.wants.percent.toFixed(0)}%</span>
                <span className="rule502030-ideal">Ideal: 30%</span>
                {statusBadge(data.wants.status, data.wants.diff)}
              </div>
              <div className="rule502030-col savings">
                <span className="rule502030-label">SAVINGS</span>
                <span className="rule502030-sub">What&apos;s left after needs and wants</span>
                <span className="rule502030-pct">{data.savings.percent.toFixed(0)}%</span>
                <span className="rule502030-ideal">Ideal: 20%</span>
                {statusBadge(data.savings.status, data.savings.diff)}
              </div>
            </div>

            <div className="rule502030-bar-wrap">
              <div className="rule502030-bar actual">
                <BarSegment
                  label="Needs"
                  percent={barSegments.needs}
                  amount={data.needs.amount}
                  className="needs"
                />
                <BarSegment
                  label="Wants"
                  percent={barSegments.wants}
                  amount={data.wants.amount}
                  className="wants"
                />
                <BarSegment
                  label="Savings"
                  percent={barSegments.savings}
                  amount={data.savings.amount}
                  className="savings"
                />
              </div>
              <p className="rule502030-bar-label">Your split</p>

              <p className="rule502030-bar-label ideal-label">Ideal:</p>
              <div className="rule502030-bar ideal">
                <div className="rule-bar-segment needs" style={{ width: "50%" }} title="Needs 50%" />
                <div className="rule-bar-segment wants" style={{ width: "30%" }} title="Wants 30%" />
                <div className="rule-bar-segment savings" style={{ width: "20%" }} title="Savings 20%" />
              </div>
            </div>

            <div className="rule502030-insights">
              <div className="rule-insight-card">
                <span className="rule-insight-icon">🏠</span>
                <p>{data.needs.insight}</p>
                {data.needs.topCategories && data.needs.topCategories.length > 0 && (
                  <p className="rule-insight-top">
                    Top: {formatTopCategories(data.needs.topCategories)}
                  </p>
                )}
              </div>
              <div className="rule-insight-card">
                <span className="rule-insight-icon">🛍️</span>
                <p>{data.wants.insight}</p>
                {data.wants.topCategories && data.wants.topCategories.length > 0 && (
                  <p className="rule-insight-top">
                    Top: {formatTopCategories(data.wants.topCategories)}
                  </p>
                )}
              </div>
              <div className="rule-insight-card">
                <span className="rule-insight-icon">💰</span>
                <p>{data.savings.insight}</p>
              </div>
            </div>

            <div className={`rule-overall-card status-${data.overallStatus.toLowerCase()}`}>
              <p>{data.overallInsight}</p>
            </div>

            {data.unclassifiedAmount > 0 && remainingUnclassified > 0 && (
              <div className="rule-unclassified">
                <button
                  type="button"
                  className="section-toggle rule-unclassified-toggle"
                  onClick={() => setUnclassifiedOpen(v => !v)}
                >
                  <span>
                    ⚠️ {remainingUnclassified} transaction
                    {remainingUnclassified === 1 ? "" : "s"} need classification
                  </span>
                  <span>{unclassifiedOpen ? "−" : "+"}</span>
                </button>
                {unclassifiedOpen && (
                  <div className="rule-unclassified-list">
                    {data.unclassifiedTransactions.map(tx => (
                      <div key={tx.id} className="rule-unclassified-row">
                        <div className="rule-unclassified-info">
                          <strong>{tx.description || tx.category}</strong>
                          <span>
                            {tx.category} · {formatInr(tx.amount)}
                          </span>
                        </div>
                        <div className="rule-classify-actions">
                          {(["NEED", "WANT", "SAVING"] as const).map(type => (
                            <button
                              key={type}
                              type="button"
                              className="btn-secondary rule-classify-btn"
                              disabled={classifyingId === tx.id}
                              onClick={() => void handleClassify(tx.id, type)}
                            >
                              {type.charAt(0) + type.slice(1).toLowerCase()}
                            </button>
                          ))}
                        </div>
                      </div>
                    ))}
                    <p className="chart-subtitle">
                      {remainingUnclassified} transaction
                      {remainingUnclassified === 1 ? "" : "s"} remaining
                    </p>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </section>

      <NetWorthSection onError={msg => setError(msg)} />
    </div>
  );
}
