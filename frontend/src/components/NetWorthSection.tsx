import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import {
  NamedAmount,
  NetWorthData,
  NetWorthUpdateRequest,
  getNetWorth,
  saveNetWorth
} from "../api";
import { CHART, CHART_TOOLTIP } from "../theme";

const MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function formatInr(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function formatMonthLabel(ym: string): string {
  const [, m] = ym.split("-");
  const idx = Number(m) - 1;
  return MONTH_LABELS[idx] ?? ym;
}

function emptyNamed(): NamedAmount {
  return { name: "", amount: 0 };
}

type Props = {
  onError?: (msg: string) => void;
};

export function NetWorthSection({ onError }: Props) {
  const [data, setData] = useState<NetWorthData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editing, setEditing] = useState(false);

  const [useAutoBankBalance, setUseAutoBankBalance] = useState(true);
  const [bankBalance, setBankBalance] = useState(0);
  const [fixedDeposits, setFixedDeposits] = useState(0);
  const [investments, setInvestments] = useState(0);
  const [physicalAssets, setPhysicalAssets] = useState<NamedAmount[]>([]);
  const [studentLoan, setStudentLoan] = useState(0);
  const [creditCardDebt, setCreditCardDebt] = useState(0);
  const [moneyOwed, setMoneyOwed] = useState<NamedAmount[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getNetWorth();
      setData(res);
      setUseAutoBankBalance(res.useAutoBankBalance);
      setBankBalance(res.bankBalance);
      setFixedDeposits(res.fixedDeposits);
      setInvestments(res.investments);
      setPhysicalAssets(res.physicalAssets.length ? res.physicalAssets : []);
      setStudentLoan(res.studentLoan);
      setCreditCardDebt(res.creditCardDebt);
      setMoneyOwed(res.moneyOwed.length ? res.moneyOwed : []);
    } catch (err: any) {
      onError?.(err.message || "Failed to load net worth");
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [onError]);

  useEffect(() => {
    void load();
  }, [load]);

  const chartData = useMemo(
    () =>
      (data?.history ?? []).map(h => ({
        ...h,
        label: formatMonthLabel(h.month)
      })),
    [data?.history]
  );

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const body: NetWorthUpdateRequest = {
        useAutoBankBalance,
        bankBalance: useAutoBankBalance ? 0 : bankBalance,
        fixedDeposits,
        investments,
        physicalAssets: physicalAssets.filter(p => p.name.trim()),
        studentLoan,
        creditCardDebt,
        moneyOwed: moneyOwed.filter(m => m.name.trim())
      };
      const res = await saveNetWorth(body);
      setData(res);
      setEditing(false);
    } catch (err: any) {
      onError?.(err.message || "Failed to save net worth");
    } finally {
      setSaving(false);
    }
  };

  const updateNamed = (
    list: NamedAmount[],
    setList: (v: NamedAmount[]) => void,
    index: number,
    field: keyof NamedAmount,
    value: string
  ) => {
    const next = [...list];
    if (field === "amount") {
      next[index] = { ...next[index], amount: Number(value) || 0 };
    } else {
      next[index] = { ...next[index], name: value };
    }
    setList(next);
  };

  if (loading) {
    return (
      <section className="card networth-section">
        <h2>Net Worth</h2>
        <p className="chart-subtitle">Loading…</p>
      </section>
    );
  }

  return (
    <section className="card networth-section">
      <div className="networth-header">
        <div>
          <h2>Net Worth</h2>
          <p className="chart-subtitle">
            The big picture — assets minus liabilities. Check in monthly.
          </p>
        </div>
        <button
          type="button"
          className="btn-secondary"
          onClick={() => setEditing(v => !v)}
        >
          {editing ? "Cancel" : "Update values"}
        </button>
      </div>

      {data && (
        <>
          <div className="networth-hero">
            <span className="networth-hero-label">Net worth</span>
            <span className={`networth-hero-value${data.netWorth < 0 ? " negative" : ""}`}>
              {formatInr(data.netWorth)}
            </span>
            <p className={`networth-mom${data.monthOverMonthChange >= 0 ? " positive" : " negative"}`}>
              {data.monthOverMonthMessage}
            </p>
          </div>

          <div className="networth-summary-row">
            <div className="networth-summary-item">
              <span className="label">Total assets</span>
              <strong className="text-green">{formatInr(data.totalAssets)}</strong>
            </div>
            <div className="networth-summary-item">
              <span className="label">Total liabilities</span>
              <strong className="text-red">{formatInr(data.totalLiabilities)}</strong>
            </div>
          </div>

          {chartData.length > 0 && (
            <div className="networth-chart-wrap">
              <span className="networth-chart-label">Net worth over time</span>
              <ResponsiveContainer width="100%" height={200}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
                  <XAxis dataKey="label" tick={{ fill: CHART.muted, fontSize: 11 }} />
                  <YAxis
                    tick={{ fill: CHART.muted, fontSize: 11 }}
                    tickFormatter={v => `₹${Math.round(v / 1000)}k`}
                  />
                  <Tooltip
                    formatter={(value) => formatInr(Number(value ?? 0))}
                    contentStyle={CHART_TOOLTIP}
                  />
                  <Line
                    type="monotone"
                    dataKey="netWorth"
                    stroke={CHART.positive}
                    strokeWidth={2}
                    dot={{ r: 3, fill: CHART.positive }}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          <div className="networth-breakdown-grid">
            <div className="networth-breakdown-card">
              <h3>Asset mix</h3>
              <div className="networth-mix-bar">
                <div
                  className="liquid"
                  style={{ width: `${data.liquidPercent}%` }}
                  title={`Liquid ${data.liquidPercent.toFixed(0)}%`}
                />
                <div
                  className="fixed"
                  style={{ width: `${data.fixedPercent}%` }}
                  title={`Fixed ${data.fixedPercent.toFixed(0)}%`}
                />
              </div>
              <p className="networth-mix-labels">
                <span>Liquid {data.liquidPercent.toFixed(0)}%</span>
                <span>Fixed {data.fixedPercent.toFixed(0)}%</span>
              </p>
              <ul className="networth-breakdown-list">
                {data.assetBreakdown.map(item => (
                  <li key={item.label}>
                    <span>{item.label}</span>
                    <span>
                      {formatInr(item.amount)} ({item.percent.toFixed(0)}%)
                    </span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="networth-breakdown-card">
              <h3>Liabilities</h3>
              {data.liabilityBreakdown.length === 0 ? (
                <p className="chart-subtitle">No liabilities recorded.</p>
              ) : (
                <ul className="networth-breakdown-list">
                  {data.liabilityBreakdown.map(item => (
                    <li key={item.label}>
                      <span>{item.label}</span>
                      <span>
                        {formatInr(item.amount)} ({item.percent.toFixed(0)}%)
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      )}

      {editing && (
        <form className="form networth-form" onSubmit={handleSubmit}>
          <h3>Assets</h3>
          <label className="check-row">
            <input
              type="checkbox"
              checked={useAutoBankBalance}
              onChange={e => setUseAutoBankBalance(e.target.checked)}
            />{" "}
            Auto-pull bank balance from transactions
            {data && useAutoBankBalance && (
              <span className="networth-auto-hint"> ({formatInr(data.autoBankBalance)})</span>
            )}
          </label>
          {!useAutoBankBalance && (
            <label>
              Bank balance (₹)
              <input
                type="number"
                min="0"
                step="0.01"
                value={bankBalance}
                onChange={e => setBankBalance(Number(e.target.value) || 0)}
              />
            </label>
          )}
          <label>
            Fixed deposits (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={fixedDeposits}
              onChange={e => setFixedDeposits(Number(e.target.value) || 0)}
            />
          </label>
          <label>
            Investments — mutual funds, stocks (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={investments}
              onChange={e => setInvestments(Number(e.target.value) || 0)}
            />
          </label>

          <div className="networth-named-list">
            <span className="networth-named-label">Physical assets</span>
            {physicalAssets.map((item, i) => (
              <div key={item.id ?? i} className="networth-named-row">
                <input
                  placeholder="e.g. Bike, Laptop"
                  value={item.name}
                  onChange={e => updateNamed(physicalAssets, setPhysicalAssets, i, "name", e.target.value)}
                />
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="₹"
                  value={item.amount || ""}
                  onChange={e => updateNamed(physicalAssets, setPhysicalAssets, i, "amount", e.target.value)}
                />
                <button
                  type="button"
                  className="link-button danger"
                  onClick={() => setPhysicalAssets(physicalAssets.filter((_, j) => j !== i))}
                >
                  Remove
                </button>
              </div>
            ))}
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setPhysicalAssets([...physicalAssets, emptyNamed()])}
            >
              + Add asset
            </button>
          </div>

          <h3>Liabilities</h3>
          <label>
            Student loan outstanding (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={studentLoan}
              onChange={e => setStudentLoan(Number(e.target.value) || 0)}
            />
          </label>
          <label>
            Credit card debt (₹)
            <input
              type="number"
              min="0"
              step="0.01"
              value={creditCardDebt}
              onChange={e => setCreditCardDebt(Number(e.target.value) || 0)}
            />
          </label>

          <div className="networth-named-list">
            <span className="networth-named-label">Money owed to someone</span>
            {moneyOwed.map((item, i) => (
              <div key={item.id ?? i} className="networth-named-row">
                <input
                  placeholder="e.g. Priya, Roommate"
                  value={item.name}
                  onChange={e => updateNamed(moneyOwed, setMoneyOwed, i, "name", e.target.value)}
                />
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="₹"
                  value={item.amount || ""}
                  onChange={e => updateNamed(moneyOwed, setMoneyOwed, i, "amount", e.target.value)}
                />
                <button
                  type="button"
                  className="link-button danger"
                  onClick={() => setMoneyOwed(moneyOwed.filter((_, j) => j !== i))}
                >
                  Remove
                </button>
              </div>
            ))}
            <button
              type="button"
              className="btn-secondary"
              onClick={() => setMoneyOwed([...moneyOwed, emptyNamed()])}
            >
              + Add liability
            </button>
          </div>

          <button className="btn-primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : "Save & snapshot"}
          </button>
        </form>
      )}
    </section>
  );
}
