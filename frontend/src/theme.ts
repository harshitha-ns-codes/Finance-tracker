/** Shared chart & accent colors — maroon & beige palette */
export const CHART = {
  income: "#722f37",
  expense: "#c4a574",
  balance: "#5c1824",
  positive: "#2d6a4f",
  negative: "#9b2c2c",
  warning: "#b8860b",
  grid: "rgba(114, 47, 55, 0.12)",
  tooltipBg: "#fffcf8",
  tooltipBorder: "rgba(114, 47, 55, 0.2)",
  tooltipText: "#2c1810",
  muted: "#8b7b72",
  salary: "#c4a574",
  categories: [
    "#722f37",
    "#8b2942",
    "#c4a574",
    "#a03250",
    "#d4b896",
    "#5c1824",
    "#b8956a",
    "#3d1219",
    "#e8d5b5",
    "#9b2c2c"
  ]
} as const;

export const ADVISOR_ACCENTS = {
  warning: "#b8860b",
  reminder: "#8b2942",
  achievement: "#2d6a4f",
  tip: "#722f37"
} as const;

export const CONFETTI_COLORS = [
  "#722f37",
  "#c4a574",
  "#8b2942",
  "#d4b896",
  "#2d6a4f"
];

export const CHART_TOOLTIP = {
  background: CHART.tooltipBg,
  border: `1px solid ${CHART.tooltipBorder}`,
  borderRadius: "0.6rem",
  color: CHART.tooltipText
} as const;
