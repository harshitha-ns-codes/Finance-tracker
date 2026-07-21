import { useEffect, useMemo, useState } from "react";
import {
  Anomaly,
  CategoryBreakdown,
  CashFlowForecast,
  DashboardSummary,
  MonthlyTrend,
  Transaction,
  getAnomalies,
  getCashFlowForecast,
  getCategoryBreakdown,
  getDashboard,
  getTrends,
  listTransactions
} from "../api";
import { AdvisorCard } from "../components/AdvisorCard";
import { CashFlowForecastPanel } from "../components/CashFlowForecastPanel";
import { StreaksSection } from "../components/StreaksSection";
import { HeroBanner } from "../components/HeroBanner";
import { CHART, CHART_TOOLTIP } from "../theme";
import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";

const MONTH_LABELS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function currentMonthValue(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function monthOptions(count = 12): string[] {
  const options: string[] = [];
  const now = new Date();
  for (let i = 0; i < count; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
  }
  return options;
}

function formatMonthLabel(ym: string): string {
  const [, month] = ym.split("-");
  const idx = Number(month) - 1;
  return MONTH_LABELS[idx] ?? ym;
}

function formatMonthLong(ym: string): string {
  const [year, month] = ym.split("-");
  const idx = Number(month) - 1;
  return `${MONTH_LABELS[idx] ?? month} ${year}`;
}

function monthBounds(ym: string): { from: string; to: string } {
  const [y, m] = ym.split("-").map(Number);
  const lastDay = new Date(y, m, 0).getDate();
  return {
    from: `${ym}-01`,
    to: `${ym}-${String(lastDay).padStart(2, "0")}`
  };
}

function formatInr(n: number): string {
  return `₹${n.toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
}

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [anomalies, setAnomalies] = useState<Anomaly[]>([]);
  const [trends, setTrends] = useState<MonthlyTrend[]>([]);
  const [categories, setCategories] = useState<CategoryBreakdown[]>([]);
  const [monthTxs, setMonthTxs] = useState<Transaction[]>([]);
  const [categoryMonth, setCategoryMonth] = useState(currentMonthValue);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [categoriesLoading, setCategoriesLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [cashFlow, setCashFlow] = useState<CashFlowForecast | null>(null);
  const [cashFlowLoading, setCashFlowLoading] = useState(false);
  const [cashFlowError, setCashFlowError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const [s, a, t] = await Promise.all([getDashboard(), getAnomalies(), getTrends(6)]);
        setSummary(s);
        setAnomalies(a);
        setTrends(t);
      } catch (err: any) {
        setError(err.message || "Failed to load dashboard");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setCashFlowLoading(true);
      setCashFlowError(null);
      try {
        const forecast = await getCashFlowForecast(currentMonthValue());
        if (!cancelled) setCashFlow(forecast);
      } catch (err: any) {
        if (!cancelled) {
          setCashFlow(null);
          setCashFlowError(err.message || "Failed to load cash flow forecast");
        }
      } finally {
        if (!cancelled) setCashFlowLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setCategoriesLoading(true);
      setSelectedCategory(null);
      try {
        const { from, to } = monthBounds(categoryMonth);
        const [cats, txs] = await Promise.all([
          getCategoryBreakdown(categoryMonth),
          listTransactions(from, to)
        ]);
        if (!cancelled) {
          setCategories(cats);
          setMonthTxs(txs.filter(t => t.type === "EXPENSE"));
        }
      } catch (err: any) {
        if (!cancelled) {
          setError(err.message || "Failed to load category breakdown");
        }
      } finally {
        if (!cancelled) setCategoriesLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [categoryMonth]);

  const chartData = useMemo(
    () =>
      trends.map(t => ({
        month: formatMonthLabel(t.month),
        monthKey: t.month,
        income: Number(t.income) || 0,
        expenses: Number(t.expenses) || 0,
        balance: (Number(t.income) || 0) - (Number(t.expenses) || 0)
      })),
    [trends]
  );

  const insights = useMemo(() => {
    if (trends.length === 0) return null;

    let highest = trends[0];
    let bestSavings = trends[0];
    let bestSaved = (Number(trends[0].income) || 0) - (Number(trends[0].expenses) || 0);

    for (const t of trends) {
      const expenses = Number(t.expenses) || 0;
      const saved = (Number(t.income) || 0) - expenses;
      if (expenses > (Number(highest.expenses) || 0)) {
        highest = t;
      }
      if (saved > bestSaved) {
        bestSaved = saved;
        bestSavings = t;
      }
    }

    return {
      highestMonth: formatMonthLong(highest.month),
      highestAmount: Number(highest.expenses) || 0,
      bestMonth: formatMonthLong(bestSavings.month),
      bestSaved
    };
  }, [trends]);

  const pieData = useMemo(
    () =>
      categories.map((c, i) => ({
        name: c.category,
        value: Number(c.amount) || 0,
        percentage: Number(c.percentage) || 0,
        color: CHART.categories[i % CHART.categories.length]
      })),
    [categories]
  );

  const categoryTotal = useMemo(
    () => pieData.reduce((sum, d) => sum + d.value, 0),
    [pieData]
  );

  const recentForCategory = useMemo(() => {
    if (!selectedCategory) return [];
    return monthTxs
      .filter(t => t.category === selectedCategory)
      .sort((a, b) => b.date.localeCompare(a.date))
      .slice(0, 8);
  }, [monthTxs, selectedCategory]);

  if (loading) {
    return <div className="page">Loading dashboard...</div>;
  }

  if (error && !summary) {
    return (
      <div className="page">
        <div className="error">{error}</div>
      </div>
    );
  }

  if (!summary) return null;

  return (
    <div className="page">
      <HeroBanner />

      <div className="dashboard-content">
        <div className="budget-page-header">
          <div>
            <h1>Home</h1>
            <p className="chart-subtitle">
              This month&apos;s numbers stay in sync with Plan spent, Transactions, and Advisor.
            </p>
          </div>
          <span className="sync-pill">Synced · {formatMonthLong(summary.month || currentMonthValue())}</span>
        </div>

        <AdvisorCard />

        <div className="grid-3">
        <div className="card metric-card income">
          <span className="label">This month income</span>
          <span className="value">{formatInr(summary.totalIncome)}</span>
          <span className="metric-hint">From Transactions</span>
        </div>
        <div className="card metric-card expense">
          <span className="label">This month spent</span>
          <span className="value">{formatInr(summary.totalExpenses)}</span>
          <span className="metric-hint">Budget spent + expense transactions</span>
        </div>
        <div className="card metric-card balance">
          <span className="label">This month left</span>
          <span className="value">{formatInr(summary.balance)}</span>
          <span className="metric-hint">Income − spent</span>
        </div>
      </div>

      <div className="card budget-progress-card">
        <div className="budget-progress-header">
          <div>
            <h2>Budget vs spent</h2>
            <p className="chart-subtitle">
              Same totals as your Budget sheet for {formatMonthLong(summary.month || currentMonthValue())}
            </p>
          </div>
          {summary.monthlyBudgetLimit ? (
            <strong>
              {formatInr(summary.monthlyExpenses ?? 0)} / {formatInr(summary.monthlyBudgetLimit)}
            </strong>
          ) : (
            <span className="chart-subtitle">No budget planned yet</span>
          )}
        </div>
        {summary.monthlyBudgetLimit && summary.monthlyBudgetLimit > 0 ? (
          <>
            <div className="budget-progress-track">
              <div
                className={`budget-progress-fill ${summary.nearBudgetLimit ? "warn" : ""}`}
                style={{
                  width: `${Math.min(
                    100,
                    ((summary.monthlyExpenses ?? 0) / summary.monthlyBudgetLimit) * 100
                  )}%`
                }}
              />
            </div>
            <p className={summary.nearBudgetLimit ? "warning" : "success"}>
              {summary.nearBudgetLimit
                ? "You are close to (or over) this month's budget limit."
                : "You are within your monthly budget."}
            </p>
          </>
        ) : (
          <p className="chart-subtitle">
            Add budget lines on the Plan page, then save spent amounts — they appear here and on Advisor.
          </p>
        )}
      </div>

      <div className="card chart-card">
        <h2>Cash flow forecast</h2>
        <p className="chart-subtitle">Projected daily balance for the rest of this month</p>
        <CashFlowForecastPanel
          forecast={cashFlow}
          loading={cashFlowLoading}
          error={cashFlowError}
        />
      </div>

      <div className="card chart-card">
        <h2>Spending trends</h2>
        <p className="chart-subtitle">Last 6 months — income vs effective spent (budget + transactions)</p>
        {chartData.length === 0 ? (
          <p>No trend data yet. Add transactions to see your chart.</p>
        ) : (
          <>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={320}>
                <ComposedChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" />
                  <XAxis
                    dataKey="month"
                    tick={{ fill: CHART.muted, fontSize: 12 }}
                    axisLine={{ stroke: CHART.grid }}
                  />
                  <YAxis
                    tick={{ fill: CHART.muted, fontSize: 12 }}
                    axisLine={{ stroke: CHART.grid }}
                    tickFormatter={v => `₹${Number(v).toLocaleString("en-IN")}`}
                    width={72}
                  />
                  <Tooltip
                    contentStyle={CHART_TOOLTIP}
                    formatter={(value, name) => [formatInr(Number(value ?? 0)), String(name ?? "")]}
                  />
                  <Legend />
                  <Bar dataKey="income" name="Income" fill={CHART.income} radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expenses" name="Expenses" fill={CHART.expense} radius={[4, 4, 0, 0]} />
                  <Line
                    type="monotone"
                    dataKey="balance"
                    name="Balance"
                    stroke={CHART.positive}
                    strokeWidth={2}
                    dot={{ r: 3, fill: CHART.positive }}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
            {insights && (
              <div className="trend-insights">
                <p>
                  Your highest spending month was <strong>{insights.highestMonth}</strong> at{" "}
                  <strong>{formatInr(insights.highestAmount)}</strong>
                </p>
                <p>
                  Your best savings month was <strong>{insights.bestMonth}</strong> at{" "}
                  <strong>{formatInr(insights.bestSaved)} saved</strong>
                </p>
              </div>
            )}
          </>
        )}
      </div>

      <div className="card chart-card">
        <div className="category-header">
          <div>
            <h2>Category breakdown</h2>
            <p className="chart-subtitle">
              Effective spent by category (Budget spent overrides + expense transactions)
            </p>
          </div>
          <label className="month-select">
            Month
            <input
              type="month"
              value={categoryMonth}
              onChange={e => setCategoryMonth(e.target.value)}
              list="category-month-options"
            />
            <datalist id="category-month-options">
              {monthOptions().map(m => (
                <option key={m} value={m} />
              ))}
            </datalist>
          </label>
        </div>

        {categoriesLoading ? (
          <p>Loading categories...</p>
        ) : pieData.length === 0 ? (
          <p>
            No spent amounts for {formatMonthLong(categoryMonth)}. Save spent on the Plan page or
            add expense transactions.
          </p>
        ) : (
          <>
            <div className="donut-layout">
              <div className="donut-chart">
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={70}
                      outerRadius={110}
                      paddingAngle={2}
                      onClick={(_, index) => {
                        const name = pieData[index]?.name;
                        if (!name) return;
                        setSelectedCategory(prev => (prev === name ? null : name));
                      }}
                    >
                      {pieData.map((entry, index) => (
                        <Cell
                          key={entry.name}
                          fill={entry.color}
                          stroke={
                            selectedCategory === entry.name ? CHART.tooltipText : "transparent"
                          }
                          strokeWidth={selectedCategory === entry.name ? 3 : 0}
                          opacity={
                            selectedCategory && selectedCategory !== entry.name ? 0.35 : 1
                          }
                          style={{ cursor: "pointer" }}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={CHART_TOOLTIP}
                      formatter={(value, _name, props) => [
                        `${formatInr(Number(value ?? 0))} (${(props as { payload?: { percentage?: number } })?.payload?.percentage ?? 0}%)`,
                        (props as { payload?: { name?: string } })?.payload?.name ?? "Category"
                      ]}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="donut-center">
                  <span className="donut-center-label">Total</span>
                  <span className="donut-center-value">{formatInr(categoryTotal)}</span>
                </div>
              </div>

              <ul className="category-legend">
                {pieData.map(entry => (
                  <li key={entry.name}>
                    <button
                      type="button"
                      className={
                        selectedCategory === entry.name
                          ? "category-legend-item active"
                          : "category-legend-item"
                      }
                      onClick={() =>
                        setSelectedCategory(prev =>
                          prev === entry.name ? null : entry.name
                        )
                      }
                    >
                      <span
                        className="category-swatch"
                        style={{ background: entry.color }}
                      />
                      <span className="category-legend-text">
                        <strong>{entry.name}</strong>
                        <span>
                          {formatInr(entry.value)} · {entry.percentage.toFixed(1)}%
                        </span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            {selectedCategory && (
              <div className="category-tx-list">
                <h3>Recent {selectedCategory} expenses</h3>
                {recentForCategory.length === 0 ? (
                  <p>
                    No transactions logged for this category — the spent amount may come from your
                    Budget sheet.
                  </p>
                ) : (
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Description</th>
                        <th>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentForCategory.map(tx => (
                        <tr key={tx.id}>
                          <td>{tx.date}</td>
                          <td>{tx.description || "—"}</td>
                          <td className="text-red">-{formatInr(tx.amount)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </>
        )}
      </div>

      <div className="grid-2">
        <div className="card">
          <h2>Top spending</h2>
          {summary.topSpendingCategory ? (
            <p>
              This month you spent the most on <strong>{summary.topSpendingCategory}</strong> (
              {formatInr(summary.topSpendingAmount ?? 0)}).
            </p>
          ) : (
            <p>No spending recorded yet for this month.</p>
          )}
        </div>
        <div className="card">
          <h2>How sync works</h2>
          <ul className="sync-help-list">
            <li>
              <strong>Budget spent</strong> and expense <strong>Transactions</strong> both count as
              spent.
            </li>
            <li>
              <strong>Overview</strong> and <strong>Health</strong> use those same totals.
            </li>
            <li>After saving spent on Budget, open Overview or Health to see it update.</li>
          </ul>
        </div>
      </div>

      <div className="card">
        <h2>Potential anomalies</h2>
        {anomalies.length === 0 ? (
          <p>No unusual transactions detected.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {anomalies.map(a => (
                <tr key={a.transactionId}>
                  <td>{a.date}</td>
                  <td>{a.category}</td>
                  <td>{formatInr(a.amount)}</td>
                  <td>{a.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <StreaksSection />
      </div>
    </div>
  );
}
