import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  BillSplit,
  BillSplitRequest,
  BillSplitSummary,
  SplitType,
  createBillSplit,
  deleteBillSplit,
  getBillSplitSummary,
  listBillSplits,
  updateBillSplitStatus
} from "../api";

const SPLIT_CATEGORIES = [
  "Food",
  "Travel",
  "Utilities",
  "Entertainment",
  "Shopping",
  "Other"
] as const;

const CATEGORY_ICONS: Record<string, string> = {
  Food: "🍕",
  Travel: "✈️",
  Utilities: "💡",
  Entertainment: "🎬",
  Shopping: "🛍️",
  Other: "💳"
};

type SplitFilter = "ALL" | "OWED_TO_ME" | "I_OWE" | "SETTLED";
type SplitMode = "50_50" | "CUSTOM" | "FULL";

function todayStr(): string {
  return new Date().toISOString().slice(0, 10);
}

function fmt(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function categoryIcon(cat: string): string {
  return CATEGORY_ICONS[cat] ?? "💳";
}

type AddSplitPanelProps = {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
};

function AddSplitPanel({ open, onClose, onSaved }: AddSplitPanelProps) {
  const [splitType, setSplitType] = useState<SplitType>("I_PAID");
  const [title, setTitle] = useState("");
  const [totalAmount, setTotalAmount] = useState("");
  const [splitMode, setSplitMode] = useState<SplitMode>("50_50");
  const [myShare, setMyShare] = useState("");
  const [theirShare, setTheirShare] = useState("");
  const [otherPersonName, setOtherPersonName] = useState("");
  const [category, setCategory] = useState<string>("Food");
  const [date, setDate] = useState(todayStr);
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const total = Number(totalAmount) || 0;
  const my = Number(myShare) || 0;
  const theirs = Number(theirShare) || 0;
  const shareSum = my + theirs;
  const sharesMismatch = total > 0 && Math.abs(shareSum - total) > 0.01;

  useEffect(() => {
    if (!open) return;
    setSplitType("I_PAID");
    setTitle("");
    setTotalAmount("");
    setSplitMode("50_50");
    setMyShare("");
    setTheirShare("");
    setOtherPersonName("");
    setCategory("Food");
    setDate(todayStr());
    setNotes("");
    setError(null);
  }, [open]);

  useEffect(() => {
    if (total <= 0) return;
    if (splitMode === "50_50") {
      const half = total / 2;
      setMyShare(half.toFixed(2));
      setTheirShare(half.toFixed(2));
    } else if (splitMode === "FULL") {
      if (splitType === "I_PAID") {
        setMyShare("0");
        setTheirShare(total.toFixed(2));
      } else {
        setMyShare(total.toFixed(2));
        setTheirShare("0");
      }
    }
  }, [total, splitMode, splitType]);

  const handleMyShareChange = (val: string) => {
    setSplitMode("CUSTOM");
    setMyShare(val);
    const n = Number(val) || 0;
    if (total > 0) setTheirShare(Math.max(0, total - n).toFixed(2));
  };

  const handleTheirShareChange = (val: string) => {
    setSplitMode("CUSTOM");
    setTheirShare(val);
    const n = Number(val) || 0;
    if (total > 0) setMyShare(Math.max(0, total - n).toFixed(2));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (sharesMismatch) {
      setError("My share and their share must equal the total bill");
      return;
    }
    if (theirs <= 0) {
      setError("Their share must be greater than zero");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body: BillSplitRequest = {
        title: title.trim(),
        totalAmount: total,
        myShare: my,
        otherPersonName: otherPersonName.trim(),
        otherPersonAmount: theirs,
        splitType,
        category,
        date,
        notes: notes.trim() || undefined
      };
      await createBillSplit(body);
      onSaved();
      onClose();
    } catch (err: any) {
      setError(err.message || "Failed to save split");
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <>
      <div className="split-panel-overlay" onClick={onClose} aria-hidden />
      <aside className="split-panel" role="dialog" aria-labelledby="split-panel-title">
        <div className="split-panel-header">
          <h2 id="split-panel-title">New Bill Split</h2>
          <button type="button" className="split-panel-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        <form className="form split-panel-form" onSubmit={handleSubmit}>
          <div className="split-field">
            <span className="split-field-label">Who paid?</span>
            <div className="split-paid-toggle">
              <button
                type="button"
                className={splitType === "I_PAID" ? "active" : ""}
                onClick={() => setSplitType("I_PAID")}
              >
                I paid the bill
              </button>
              <button
                type="button"
                className={splitType === "THEY_PAID" ? "active" : ""}
                onClick={() => setSplitType("THEY_PAID")}
              >
                They paid the bill
              </button>
            </div>
          </div>

          <label>
            What was this for?
            <input
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="Dinner, Uber, Groceries..."
              required
            />
          </label>

          <label>
            Total bill (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={totalAmount}
              onChange={e => setTotalAmount(e.target.value)}
              required
            />
          </label>

          <div className="split-field">
            <span className="split-field-label">Split</span>
            <div className="split-mode-pills">
              <button
                type="button"
                className={splitMode === "50_50" ? "active" : ""}
                onClick={() => setSplitMode("50_50")}
              >
                50/50
              </button>
              <button
                type="button"
                className={splitMode === "CUSTOM" ? "active" : ""}
                onClick={() => setSplitMode("CUSTOM")}
              >
                Custom
              </button>
              <button
                type="button"
                className={splitMode === "FULL" ? "active" : ""}
                onClick={() => setSplitMode("FULL")}
              >
                I paid full
              </button>
            </div>
            {(splitMode === "CUSTOM" || splitMode === "50_50") && (
              <div className="split-custom-shares">
                <label>
                  My share (₹)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={myShare}
                    onChange={e => handleMyShareChange(e.target.value)}
                  />
                </label>
                <label>
                  Their share (₹)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={theirShare}
                    onChange={e => handleTheirShareChange(e.target.value)}
                  />
                </label>
                <p className={`split-total-hint${sharesMismatch ? " warn" : ""}`}>
                  Total: {fmt(shareSum)}
                  {sharesMismatch && total > 0 && ` (should be ${fmt(total)})`}
                </p>
              </div>
            )}
          </div>

          <label>
            Who did you split with?
            <input
              value={otherPersonName}
              onChange={e => setOtherPersonName(e.target.value)}
              placeholder="Priya, Roommate, Arjun..."
              required
            />
          </label>

          <label>
            Category
            <select value={category} onChange={e => setCategory(e.target.value)}>
              {SPLIT_CATEGORIES.map(c => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>

          <label>
            Date
            <input type="date" value={date} onChange={e => setDate(e.target.value)} required />
          </label>

          <label>
            Notes (optional)
            <textarea
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="Any additional context..."
              rows={3}
            />
          </label>

          {error && <div className="error">{error}</div>}

          <button className="btn-primary split-save-btn" type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save Split"}
          </button>
        </form>
      </aside>
    </>
  );
}

type SplitCardProps = {
  split: BillSplit;
  exiting?: boolean;
  onRemind: (id: string) => void;
  onSettle: (split: BillSplit) => void;
  onDelete: (id: string) => void;
  confirmSettleId: string | null;
  onConfirmSettle: (id: string) => void;
  onCancelSettle: () => void;
  actionLoading: boolean;
};

function SplitCard({
  split,
  exiting,
  onRemind,
  onSettle,
  onDelete,
  confirmSettleId,
  onConfirmSettle,
  onCancelSettle,
  actionLoading
}: SplitCardProps) {
  const isSettled = split.status === "SETTLED";
  const isOwedToMe = split.splitType === "I_PAID";
  const borderClass = isSettled ? "border-grey" : isOwedToMe ? "border-green" : "border-red";

  return (
    <div className={`split-card-wrap${exiting ? " split-card-exit" : ""}`}>
      {split.overdue && !isSettled && (
        <div className="split-overdue-badge">
          ⚠️ {split.daysAgo} days overdue
        </div>
      )}
      <div className={`split-card ${borderClass}${isSettled ? " settled" : ""}`}>
        <div className="split-card-row1">
          <div className="split-card-left">
            <span className="split-category-icon">{categoryIcon(split.category)}</span>
            <span className="split-card-date">{isSettled ? split.settledDate : split.date}</span>
          </div>
          <div className="split-card-center">
            <strong>{split.title}</strong>
            <span className="split-person-name">{split.otherPersonName}</span>
            {split.notes && <em className="split-notes-preview">{split.notes}</em>}
            {isSettled && (
              <span className="split-settled-label">Settled {split.settledDate}</span>
            )}
          </div>
          <div className="split-card-right">
            {isOwedToMe ? (
              <>
                <span className="split-amount text-green">+{fmt(split.otherPersonAmount)}</span>
                <small>they owe you</small>
              </>
            ) : (
              <>
                <span className="split-amount text-red">-{fmt(split.otherPersonAmount)}</span>
                <small>you owe them</small>
              </>
            )}
          </div>
        </div>

        {!isSettled && (
          <div className="split-card-row2">
            {confirmSettleId === split.id ? (
              <div className="split-settle-confirm">
                <p>
                  Mark as settled? This will add a {fmt(split.otherPersonAmount)}{" "}
                  {isOwedToMe ? "income" : "expense"} transaction.
                </p>
                <div className="split-settle-confirm-actions">
                  <button
                    type="button"
                    className="btn-primary"
                    disabled={actionLoading}
                    onClick={() => onConfirmSettle(split.id)}
                  >
                    Yes, settle
                  </button>
                  <button
                    type="button"
                    className="btn-secondary"
                    disabled={actionLoading}
                    onClick={onCancelSettle}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="split-card-actions">
                  {isOwedToMe && split.status === "PENDING" && (
                    <button
                      type="button"
                      className="btn-secondary split-action-btn"
                      disabled={actionLoading}
                      onClick={() => onRemind(split.id)}
                    >
                      🔔 Remind
                    </button>
                  )}
                  {split.status === "REMINDED" && (
                    <span className="split-reminded-badge">Reminded ✓</span>
                  )}
                  <button
                    type="button"
                    className="btn-primary split-action-btn"
                    disabled={actionLoading}
                    onClick={() => onSettle(split)}
                  >
                    ✓ Mark Settled
                  </button>
                </div>
                {split.status === "PENDING" && (
                  <button
                    type="button"
                    className="link-button danger split-delete-btn"
                    disabled={actionLoading}
                    onClick={() => onDelete(split.id)}
                  >
                    🗑️ Delete
                  </button>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export function BillSplitsTab() {
  const [splits, setSplits] = useState<BillSplit[]>([]);
  const [summary, setSummary] = useState<BillSplitSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<SplitFilter>("ALL");
  const [panelOpen, setPanelOpen] = useState(false);
  const [settledExpanded, setSettledExpanded] = useState(false);
  const [confirmSettleId, setConfirmSettleId] = useState<string | null>(null);
  const [exitingIds, setExitingIds] = useState<Set<string>>(new Set());
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [splitData, summaryData] = await Promise.all([
        listBillSplits(),
        getBillSplitSummary()
      ]);
      setSplits(splitData);
      setSummary(summaryData);
    } catch (err: any) {
      setError(err.message || "Failed to load splits");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const owedToMeCount = useMemo(
    () =>
      splits.filter(
        s => s.status !== "SETTLED" && s.splitType === "I_PAID"
      ).length,
    [splits]
  );

  const iOweCount = useMemo(
    () =>
      splits.filter(
        s => s.status !== "SETTLED" && s.splitType === "THEY_PAID"
      ).length,
    [splits]
  );

  const filteredActive = useMemo(() => {
    let list = splits.filter(s => s.status !== "SETTLED");
    if (filter === "OWED_TO_ME") list = list.filter(s => s.splitType === "I_PAID");
    if (filter === "I_OWE") list = list.filter(s => s.splitType === "THEY_PAID");
    return list;
  }, [splits, filter]);

  const settledSplits = useMemo(
    () => splits.filter(s => s.status === "SETTLED"),
    [splits]
  );

  const showSettled = filter === "ALL" || filter === "SETTLED";

  const handleRemind = async (id: string) => {
    setActionLoading(true);
    try {
      await updateBillSplitStatus(id, "REMINDED");
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to update");
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmSettle = async (id: string) => {
    setActionLoading(true);
    setExitingIds(prev => new Set(prev).add(id));
    window.setTimeout(async () => {
      try {
        await updateBillSplitStatus(id, "SETTLED");
        setConfirmSettleId(null);
        await load();
      } catch (err: any) {
        setError(err.message || "Failed to settle");
        setExitingIds(prev => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      } finally {
        setActionLoading(false);
      }
    }, 300);
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this split?")) return;
    setActionLoading(true);
    try {
      await deleteBillSplit(id);
      await load();
    } catch (err: any) {
      setError(err.message || "Failed to delete");
    } finally {
      setActionLoading(false);
    }
  };

  const netPositive = (summary?.netBalance ?? 0) >= 0;

  return (
    <div className="splits-tab">
      {summary && (
        <div className="split-summary-bar">
          <div className="split-summary-card owed-to-me">
            <span className="split-summary-value text-green">{fmt(summary.totalOwedToMe)}</span>
            <span className="split-summary-label">owed to you</span>
            <span className="split-summary-sub">from {owedToMeCount} pending splits</span>
          </div>
          <div className="split-summary-card i-owe">
            <span className="split-summary-value text-red">{fmt(summary.totalIOwe)}</span>
            <span className="split-summary-label">you owe</span>
            <span className="split-summary-sub">across {iOweCount} splits</span>
          </div>
          <div className={`split-summary-card net ${netPositive ? "positive" : "negative"}`}>
            <span className={`split-summary-value ${netPositive ? "text-green" : "text-red"}`}>
              {netPositive ? "" : "−"}
              {fmt(Math.abs(summary.netBalance))}
            </span>
            <span className="split-summary-label">net balance</span>
            <span className="split-summary-sub">overall position</span>
          </div>
        </div>
      )}

      <div className="splits-toolbar">
        <div className="split-filter-pills">
          {(
            [
              ["ALL", "All"],
              ["OWED_TO_ME", "Owed to me"],
              ["I_OWE", "I owe"],
              ["SETTLED", "Settled"]
            ] as const
          ).map(([key, label]) => (
            <button
              key={key}
              type="button"
              className={filter === key ? "active" : ""}
              onClick={() => setFilter(key)}
            >
              {label}
            </button>
          ))}
        </div>
        <button type="button" className="btn-primary" onClick={() => setPanelOpen(true)}>
          Add Split +
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {loading ? (
        <p>Loading splits...</p>
      ) : splits.length === 0 ? (
        <div className="split-empty-state">
          <span className="split-empty-emoji">💸</span>
          <h3>No splits tracked yet</h3>
          <p>Track shared expenses with friends, roommates, or family</p>
          <button type="button" className="btn-primary" onClick={() => setPanelOpen(true)}>
            Add Split +
          </button>
        </div>
      ) : (
        <>
          {filter !== "SETTLED" && (
            <div className="split-list">
              {filteredActive.length === 0 ? (
                <p className="chart-subtitle">No active splits in this filter.</p>
              ) : (
                filteredActive.map(split => (
                  <SplitCard
                    key={split.id}
                    split={split}
                    exiting={exitingIds.has(split.id)}
                    onRemind={handleRemind}
                    onSettle={s => setConfirmSettleId(s.id)}
                    onDelete={handleDelete}
                    confirmSettleId={confirmSettleId}
                    onConfirmSettle={handleConfirmSettle}
                    onCancelSettle={() => setConfirmSettleId(null)}
                    actionLoading={actionLoading}
                  />
                ))
              )}
            </div>
          )}

          {showSettled && settledSplits.length > 0 && (
            <div className="split-settled-section">
              <button
                type="button"
                className="split-settled-toggle"
                onClick={() => setSettledExpanded(v => !v)}
              >
                Settled ({settledSplits.length}) {settledExpanded ? "▾" : "▸"}
              </button>
              {settledExpanded && (
                <div className="split-list">
                  {settledSplits.map(split => (
                    <SplitCard
                      key={split.id}
                      split={split}
                      onRemind={handleRemind}
                      onSettle={() => {}}
                      onDelete={handleDelete}
                      confirmSettleId={null}
                      onConfirmSettle={() => {}}
                      onCancelSettle={() => {}}
                      actionLoading={actionLoading}
                    />
                  ))}
                </div>
              )}
            </div>
          )}

          {filter === "SETTLED" && settledSplits.length === 0 && (
            <p className="chart-subtitle">No settled splits yet.</p>
          )}
        </>
      )}

      <AddSplitPanel open={panelOpen} onClose={() => setPanelOpen(false)} onSaved={load} />
    </div>
  );
}
