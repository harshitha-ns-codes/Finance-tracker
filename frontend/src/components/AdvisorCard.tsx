import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { DailyInsight, getDailyInsight } from "../api";
import { ADVISOR_ACCENTS } from "../theme";

function todayDismissKey(): string {
  const now = new Date();
  const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(
    now.getDate()
  ).padStart(2, "0")}`;
  return `advisor_dismissed_${date}`;
}

function isDismissedToday(): boolean {
  try {
    return localStorage.getItem(todayDismissKey()) === "true";
  } catch {
    return false;
  }
}

function dismissToday() {
  try {
    localStorage.setItem(todayDismissKey(), "true");
  } catch {
    // ignore
  }
}

type TypeMeta = {
  icon: string;
  className: string;
  label: string;
  accent: string;
};

function typeMeta(type: string): TypeMeta {
  switch (type) {
    case "WARNING":
      return {
        icon: "⚠️",
        className: "advisor-warning",
        label: "WARNING",
        accent: ADVISOR_ACCENTS.warning
      };
    case "REMINDER":
      return {
        icon: "🔔",
        className: "advisor-reminder",
        label: "REMINDER",
        accent: ADVISOR_ACCENTS.reminder
      };
    case "ACHIEVEMENT":
      return {
        icon: "🏆",
        className: "advisor-achievement",
        label: "ACHIEVEMENT",
        accent: ADVISOR_ACCENTS.achievement
      };
    default:
      return {
        icon: "💡",
        className: "advisor-tip",
        label: "TIP",
        accent: ADVISOR_ACCENTS.tip
      };
  }
}

export function AdvisorCard() {
  const [insight, setInsight] = useState<DailyInsight | null>(null);
  const [hidden, setHidden] = useState(isDismissedToday);
  const [loading, setLoading] = useState(!isDismissedToday());

  useEffect(() => {
    if (hidden) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await getDailyInsight();
        if (!cancelled) setInsight(data);
      } catch {
        if (!cancelled) setInsight(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [hidden]);

  if (hidden) {
    return null;
  }

  if (loading) {
    return <div className="advisor-card advisor-card-skeleton" aria-hidden />;
  }

  if (!insight?.insight) {
    return null;
  }

  const meta = typeMeta(insight.type);
  const route = insight.actionRoute ?? null;

  return (
    <div className={`advisor-card ${meta.className}`}>
      <div className="advisor-card-main">
        <span className="advisor-card-icon" aria-hidden>
          {meta.icon}
        </span>
        <div className="advisor-card-body">
          <span className="advisor-card-label">{meta.label} · TODAY</span>
          <p className="advisor-card-insight">{insight.insight}</p>
          {insight.action && route && (
            <Link
              className="advisor-card-action"
              to={route}
              style={{ color: meta.accent }}
            >
              {insight.action} →
            </Link>
          )}
        </div>
      </div>
      <button
        type="button"
        className="advisor-card-dismiss"
        aria-label="Dismiss today's advisor"
        onClick={() => {
          dismissToday();
          setHidden(true);
        }}
      >
        ×
      </button>
    </div>
  );
}
