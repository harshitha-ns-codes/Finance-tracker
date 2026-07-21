export const BUDGET_CATEGORIES = [
  "Food",
  "Transport",
  "Shopping",
  "Entertainment",
  "Education",
  "Investments/Savings",
  "Miscellaneous"
] as const;

export type BudgetCategory = (typeof BUDGET_CATEGORIES)[number];

export const CATEGORY_HINTS: Record<BudgetCategory, string> = {
  Food: "Groceries, restaurants",
  Transport: "Fuel, bus, cab",
  Shopping: "Clothes, Amazon",
  Entertainment: "Movies, subscriptions",
  Education: "Courses, books",
  "Investments/Savings": "SIP, emergency fund",
  Miscellaneous: "Rent, hostel, internet, and everything else"
};
