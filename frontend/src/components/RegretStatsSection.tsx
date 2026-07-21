import { useEffect, useState } from "react";
import { RegretStats, getRegretStats } from "../api";

function fmtMoney(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function rateColor(rate: number): string {
  if (rate > 50) return "regret-color-bad";
  if (rate >= 30) return "regret-color-warn";
  return "regret-color-good";
}

function insightBorder(rate: number): string {
  if (rate > 50) return "regret-insight-bad";
  if (rate >= 30) return "regret-insight-warn";
  return "regret-insight-good";
}

export function RegretStatsSection() {
  const [stats, setStats] = useState<RegretStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getRegretStats();
        if (!cancelled) setStats(data);
      } catch {
        if (!cancelled) setStats(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading || !stats || stats.totalReviewed < 3) {
    return null;
  }

  const regretRateClass = rateColor(stats.regretRate);

  return (
    <section className="regret-stats-section card">
      <h2>Purchase Regret Insights</h2>

      <div className="regret-headline-stats">
        <div className="regret-stat-card">
          <span className={`regret-stat-value ${regretRateClass}`}>
            {Math.round(stats.regretRate)}%
          </span>
          <span className="regret-stat-label">regret rate</span>
          <span className="regret-stat-sub">
            {stats.totalRegret} of {stats.totalReviewed} purchases
          </span>
        </div>
        <div className="regret-stat-card">
          <span className="regret-stat-value regret-color-bad">
            {fmtMoney(stats.totalMoneyRegretted)}
          </span>
          <span className="regret-stat-label">money regretted</span>
          <span className="regret-stat-sub">
            avg {fmtMoney(stats.averageRegrettedAmount)} per regret
          </span>
        </div>
        <div className="regret-stat-card">
          <span className="regret-stat-value regret-color-warn">
            {stats.mostRegrettedCategory}
          </span>
          <span className="regret-stat-label">most regretted</span>
          <span className="regret-stat-sub">category to watch</span>
        </div>
        <div className="regret-stat-card">
          <span className="regret-stat-value regret-color-good">
            {stats.mostValuedCategory}
          </span>
          <span className="regret-stat-label">most valued</span>
          <span className="regret-stat-sub">keep spending here</span>
        </div>
      </div>

      {stats.regretByCategory.length > 0 && (
        <div className="regret-category-table-wrap">
          <table className="table regret-category-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Reviewed</th>
                <th>Regret</th>
                <th>Regret Rate</th>
              </tr>
            </thead>
            <tbody>
              {stats.regretByCategory.map(row => (
                <tr key={row.category}>
                  <td>{row.category}</td>
                  <td>{row.reviewed} purchases</td>
                  <td>{row.regret} regretted</td>
                  <td>
                    <div className="regret-rate-cell">
                      <div className="regret-rate-bar-track">
                        <div
                          className={`regret-rate-bar-fill ${rateColor(row.regretRate)}`}
                          style={{ width: `${Math.min(100, row.regretRate)}%` }}
                        />
                      </div>
                      <span>{Math.round(row.regretRate)}%</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className={`regret-insight-card ${insightBorder(stats.regretRate)}`}>
        <p>{stats.recentInsight}</p>
      </div>
    </section>
  );
}
