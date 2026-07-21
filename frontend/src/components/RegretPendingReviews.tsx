import { useCallback, useEffect, useState } from "react";
import {
  PendingRegret,
  RegretReviewStatus,
  getPendingRegrets,
  submitRegretReview
} from "../api";

const CATEGORY_ICONS: Record<string, string> = {
  Shopping: "🛍️",
  Food: "🍕",
  Entertainment: "🎬",
  Travel: "✈️"
};

function categoryIcon(category: string): string {
  return CATEGORY_ICONS[category] ?? "💳";
}

function daysAgo(dateStr: string): number {
  const purchase = new Date(dateStr + "T00:00:00");
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diff = today.getTime() - purchase.getTime();
  return Math.max(0, Math.floor(diff / (1000 * 60 * 60 * 24)));
}

function daysLabel(days: number): string {
  if (days === 0) return "today";
  if (days === 1) return "1 day ago";
  return `${days} days ago`;
}

type Props = {
  onReviewed?: () => void;
};

export function RegretPendingReviews({ onReviewed }: Props) {
  const [pending, setPending] = useState<PendingRegret[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAll, setShowAll] = useState(false);
  const [removingIds, setRemovingIds] = useState<Set<number>>(new Set());
  const [submittingId, setSubmittingId] = useState<number | null>(null);
  const [toast, setToast] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPendingRegrets();
      setPending(data);
    } catch {
      setPending([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleRate = async (id: number, status: RegretReviewStatus) => {
    if (submittingId != null) return;
    setSubmittingId(id);
    setRemovingIds(prev => new Set(prev).add(id));

    window.setTimeout(async () => {
      try {
        await submitRegretReview(id, status);
        setPending(prev => prev.filter(p => p.id !== id));
        setToast(true);
        window.setTimeout(() => setToast(false), 2000);
        onReviewed?.();
      } catch {
        setRemovingIds(prev => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      } finally {
        setSubmittingId(null);
      }
    }, 300);
  };

  if (loading || pending.length === 0) {
    return null;
  }

  const visible = showAll ? pending : pending.slice(0, 3);
  const hiddenCount = pending.length - 3;

  return (
    <section className="regret-pending-section card">
      {toast && <div className="regret-toast">Noted ✓</div>}
      <div className="regret-pending-header">
        <h2>Rate your recent purchases</h2>
        <p className="chart-subtitle">A week later — were these worth it?</p>
      </div>
      <div className="regret-pending-list">
        {visible.map(item => {
          const days = daysAgo(item.date);
          const removing = removingIds.has(item.id);
          return (
            <div
              key={item.id}
              className={`regret-pending-card${removing ? " regret-card-exit" : ""}`}
            >
              <div className="regret-pending-left">
                <span className="regret-category-icon" aria-hidden>
                  {categoryIcon(item.category)}
                </span>
                <span className="regret-purchase-date">{item.date}</span>
              </div>
              <div className="regret-pending-center">
                <strong>{item.description || item.category}</strong>
                <span>
                  {item.category} · ₹{Number(item.amount).toLocaleString("en-IN")}
                </span>
                <small>You bought this {daysLabel(days)}</small>
              </div>
              <div className="regret-rating-buttons">
                <button
                  type="button"
                  className="regret-rate-btn regret-rate-regret"
                  disabled={submittingId != null}
                  onClick={() => handleRate(item.id, "REGRET")}
                >
                  <span className="regret-rate-emoji">😞</span>
                  <span className="regret-rate-label">Waste of money</span>
                </button>
                <button
                  type="button"
                  className="regret-rate-btn regret-rate-neutral"
                  disabled={submittingId != null}
                  onClick={() => handleRate(item.id, "NEUTRAL")}
                >
                  <span className="regret-rate-emoji">😐</span>
                  <span className="regret-rate-label">It was okay</span>
                </button>
                <button
                  type="button"
                  className="regret-rate-btn regret-rate-worth"
                  disabled={submittingId != null}
                  onClick={() => handleRate(item.id, "NO_REGRET")}
                >
                  <span className="regret-rate-emoji">😊</span>
                  <span className="regret-rate-label">No regrets</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
      {!showAll && hiddenCount > 0 && (
        <button
          type="button"
          className="link-button regret-show-more"
          onClick={() => setShowAll(true)}
        >
          and {hiddenCount} more…
        </button>
      )}
    </section>
  );
}
