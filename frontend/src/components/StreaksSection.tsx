import { useEffect, useMemo, useState } from "react";
import { LoggingDay, StreakMetric, StreakType, getStreaks } from "../api";

const STREAK_ICONS: Record<StreakType, string> = {
  UNDER_BUDGET: "📊",
  LOGGING: "📝",
  SAVING: "🐷",
  BILL_PAYMENT: "📅"
};

function unitLabel(count: number, unit: string): string {
  if (unit === "months") {
    return count === 1 ? "month" : "months";
  }
  return count === 1 ? "day" : "days";
}

function LoggingHeatmap({ days }: { days: LoggingDay[] }) {
  const weeks = useMemo(() => {
    const result: LoggingDay[][] = [];
    let week: LoggingDay[] = [];
    for (let i = 0; i < days.length; i++) {
      week.push(days[i]);
      if (week.length === 7) {
        result.push(week);
        week = [];
      }
    }
    if (week.length > 0) {
      result.push(week);
    }
    return result;
  }, [days]);

  const intensity = (count: number, logged: boolean): string => {
    if (!logged) return "level-0";
    if (count >= 3) return "level-3";
    if (count >= 2) return "level-2";
    return "level-1";
  };

  return (
    <div className="streak-heatmap-wrap">
      <div className="streak-heatmap" aria-label="Transaction logging activity">
        {weeks.map((week, wi) => (
          <div key={wi} className="streak-heatmap-week">
            {week.map(day => (
              <span
                key={day.date}
                className={`streak-heatmap-cell ${intensity(day.count, day.logged)}`}
                title={`${day.date}${day.logged ? `: ${day.count} transaction(s)` : ": no logs"}`}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="streak-heatmap-legend">
        <span>Less</span>
        <span className="streak-heatmap-cell level-0" />
        <span className="streak-heatmap-cell level-1" />
        <span className="streak-heatmap-cell level-2" />
        <span className="streak-heatmap-cell level-3" />
        <span>More</span>
      </div>
    </div>
  );
}

function StreakCard({ streak }: { streak: StreakMetric }) {
  const icon = STREAK_ICONS[streak.type];
  const active = streak.current > 0;

  return (
    <div
      className={`streak-card${streak.broken ? " streak-broken" : ""}${streak.atRisk ? " streak-at-risk" : ""}`}
    >
      <div className="streak-card-top">
        <span className="streak-icon" aria-hidden>
          {icon}
        </span>
        <div className="streak-card-meta">
          <span className="streak-label">{streak.label}</span>
          <span className={`streak-count${active ? " active" : ""}`}>
            {streak.current}{" "}
            <small>{unitLabel(streak.current, streak.unit)}</small>
          </span>
        </div>
      </div>
      <div className="streak-card-bottom">
        <span className="streak-best">Best: {streak.best}</span>
        {streak.broken && streak.brokenMessage && (
          <span className="streak-broken-msg">{streak.brokenMessage}</span>
        )}
        {!streak.broken && streak.atRisk && streak.brokenMessage && (
          <span className="streak-risk-msg">{streak.brokenMessage}</span>
        )}
      </div>
    </div>
  );
}

export function StreaksSection() {
  const [data, setData] = useState<{ streaks: StreakMetric[]; loggingHeatmap: LoggingDay[] } | null>(
    null
  );
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await getStreaks();
        if (!cancelled) setData(res);
      } catch {
        if (!cancelled) setData(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <section className="streaks-section card streaks-section-compact">
        <h2>Your streaks</h2>
        <p className="chart-subtitle">Loading streaks…</p>
      </section>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <section className="streaks-section card streaks-section-compact">
      <div className="streaks-section-header">
        <h2>Your streaks</h2>
        <p className="chart-subtitle">Small wins add up — log daily to build momentum.</p>
      </div>

      <div className="streak-cards-grid">
        {data.streaks.map(s => (
          <StreakCard key={s.type} streak={s} />
        ))}
      </div>

      {data.loggingHeatmap.length > 0 && (
        <div className="streak-heatmap-section">
          <span className="streak-heatmap-label">Logging activity</span>
          <LoggingHeatmap days={data.loggingHeatmap} />
        </div>
      )}
    </section>
  );
}
