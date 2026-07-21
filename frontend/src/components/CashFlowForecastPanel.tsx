import { useMemo } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { CashFlowForecast } from "../api";
import { CHART } from "../theme";

function formatInr(n: number): string {
  return `₹${Math.round(n).toLocaleString("en-IN")}`;
}

function shortDate(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

function longDate(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString("en-IN", { month: "long", day: "numeric" });
}

type ChartPoint = {
  date: string;
  label: string;
  balance: number;
  positive: number | null;
  negative: number | null;
  events: string[];
  hasEvent: boolean;
};

function CashFlowTooltip({
  active,
  payload
}: {
  active?: boolean;
  payload?: Array<{ payload: ChartPoint }>;
}) {
  if (!active || !payload?.length) return null;
  const point = payload[0].payload;
  return (
    <div className="cashflow-tooltip">
      <strong>{longDate(point.date)}</strong>
      <div>Projected: {formatInr(point.balance)}</div>
      {point.events.length > 0 ? (
        <ul>
          {point.events.map(e => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      ) : (
        <p className="chart-subtitle">No bills due</p>
      )}
    </div>
  );
}

function EventDot(props: {
  cx?: number;
  cy?: number;
  payload?: ChartPoint;
}) {
  const { cx, cy, payload } = props;
  if (cx == null || cy == null || !payload?.hasEvent) return null;
  return (
    <g>
      <circle cx={cx} cy={cy} r={5} fill={CHART.warning} stroke={CHART.tooltipBg} strokeWidth={2} />
      <line
        x1={cx}
        y1={cy + 6}
        x2={cx}
        y2={cy + 18}
        stroke={CHART.warning}
        strokeWidth={1.5}
        strokeDasharray="3 3"
      />
    </g>
  );
}

export function CashFlowForecastPanel({
  forecast,
  loading,
  error
}: {
  forecast: CashFlowForecast | null;
  loading: boolean;
  error: string | null;
}) {
  const chartData = useMemo<ChartPoint[]>(() => {
    if (!forecast?.days?.length) return [];
    return forecast.days.map((d, i, arr) => {
      const balance = Number(d.projectedBalance) || 0;
      const prev = i > 0 ? Number(arr[i - 1].projectedBalance) || 0 : balance;
      const crosses = (prev >= 0 && balance < 0) || (prev < 0 && balance >= 0);
      return {
        date: d.date,
        label: shortDate(d.date),
        balance,
        positive: balance >= 0 || crosses ? balance : null,
        negative: balance < 0 || crosses ? balance : null,
        events: d.events || [],
        hasEvent: (d.events || []).length > 0
      };
    });
  }, [forecast]);

  const firstNegative = forecast?.negativeDates?.[0] ?? null;
  const callout = useMemo(() => {
    if (!forecast?.lowestPoint?.date) {
      return forecast?.summary || "No remaining days to forecast for this month.";
    }
    if (!forecast.willGoNegative) {
      return (
        forecast.summary ||
        `Your balance stays positive all month. Lowest point: ${formatInr(
          forecast.lowestPoint.balance
        )} on ${longDate(forecast.lowestPoint.date)}.`
      );
    }
    return (
      forecast.summary ||
      `Balance may go negative on ${longDate(
        firstNegative || forecast.lowestPoint.date
      )}. Consider delaying an optional expense.`
    );
  }, [forecast, firstNegative]);

  return (
    <div className="cashflow-panel">
      <p className="chart-subtitle">
        Projected daily balance for the rest of the month from unpaid bills, salary day,
        subscriptions, and planned purchases.
      </p>

      {loading && <p>Loading cash flow forecast…</p>}
      {error && <div className="error">{error}</div>}

      {!loading && !error && chartData.length === 0 && (
        <p className="chart-subtitle">{callout}</p>
      )}

      {!loading && chartData.length > 0 && (
        <>
          <div className="cashflow-meta">
            <span>
              Starting balance: <strong>{formatInr(forecast?.startingBalance || 0)}</strong>
            </span>
            <span className="cashflow-legend">
              <i className="dot-event" /> Day with bills / income
            </span>
          </div>

          <div className="cashflow-chart">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chartData} margin={{ top: 12, right: 16, left: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
                <XAxis
                  dataKey="label"
                  tick={{ fill: CHART.muted, fontSize: 11 }}
                  interval="preserveStartEnd"
                  minTickGap={28}
                />
                <YAxis
                  tick={{ fill: CHART.muted, fontSize: 11 }}
                  tickFormatter={v => `₹${Number(v).toLocaleString("en-IN")}`}
                  width={72}
                />
                <Tooltip content={<CashFlowTooltip />} />
                <ReferenceLine y={0} stroke="rgba(155, 44, 44, 0.45)" strokeDasharray="4 4" />
                {firstNegative && (
                  <ReferenceLine
                    x={shortDate(firstNegative)}
                    stroke="rgba(155, 44, 44, 0.35)"
                    strokeDasharray="3 3"
                  />
                )}
                <Line
                  type="monotone"
                  dataKey="positive"
                  name="Balance"
                  stroke={CHART.positive}
                  strokeWidth={2.5}
                  dot={<EventDot />}
                  activeDot={{ r: 6 }}
                  connectNulls={false}
                />
                <Line
                  type="monotone"
                  dataKey="negative"
                  name="Negative"
                  stroke={CHART.negative}
                  strokeWidth={2.5}
                  dot={false}
                  activeDot={{ r: 6 }}
                  connectNulls={false}
                  legendType="none"
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div
            className={`cashflow-callout ${
              forecast?.willGoNegative ? "risky" : "safe"
            }`}
          >
            {forecast?.willGoNegative ? `⚠️ ${callout}` : callout}
          </div>
        </>
      )}
    </div>
  );
}
