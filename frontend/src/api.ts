import { clearToken } from "./auth";
import {
  ApiError,
  apiClient,
  handleUnauthorized,
  isAxiosError,
  publicClient,
  readAxiosError,
  toAxiosConfig
} from "./api/client";

async function publicRequest<T>(
  path: string,
  options: RequestInit = {},
  errorOptions: { authRequest?: boolean } = {}
): Promise<T> {
  try {
    const config = toAxiosConfig(options);
    const res = await publicClient.request<T>({ url: path, ...config });
    return res.data;
  } catch (error) {
    if (isAxiosError(error)) {
      throw readAxiosError(error, errorOptions);
    }
    throw error;
  }
}

function assertAuthToken(data: unknown): { token: string } {
  if (
    data &&
    typeof data === "object" &&
    "token" in data &&
    typeof (data as { token: unknown }).token === "string" &&
    (data as { token: string }).token.length > 0
  ) {
    return data as { token: string };
  }

  throw new ApiError(
    import.meta.env.PROD
      ? "Sign-in succeeded but no token was returned. Check VITE_API_BASE_URL points to the API, not the frontend."
      : "Sign-in succeeded but no token was returned. Is the backend running?"
  );
}

export async function checkApiReachable(): Promise<boolean> {
  try {
    await publicClient.get("/health", { timeout: 8_000 });
    return true;
  } catch {
    return false;
  }
}

type RequestOptions = RequestInit & { redirectOnAuthError?: boolean };

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { redirectOnAuthError = true, ...fetchOptions } = options;

  try {
    const config = toAxiosConfig(fetchOptions);
    const res = await apiClient.request<T>({
      url: path,
      ...config,
      validateStatus: status => status < 400 || status === 204
    });

    if (res.status === 204) {
      return {} as T;
    }

    return res.data;
  } catch (error) {
    if (isAxiosError(error)) {
      if (error.response?.status === 401 && redirectOnAuthError) {
        handleUnauthorized();
      }
      throw readAxiosError(error);
    }
    throw error;
  }
}

export async function login(username: string, password: string) {
  clearToken();
  const data = await publicRequest<{ token: string }>(
    "/auth/login",
    {
      method: "POST",
      body: JSON.stringify({
        username: username.trim(),
        password
      })
    },
    { authRequest: true }
  );
  return assertAuthToken(data);
}

export async function register(username: string, email: string, password: string) {
  clearToken();
  const data = await publicRequest<{ token: string }>(
    "/auth/register",
    {
      method: "POST",
      body: JSON.stringify({
        username: username.trim(),
        email: email.trim(),
        password
      })
    },
    { authRequest: true }
  );
  return assertAuthToken(data);
}

export type TransactionType = "INCOME" | "EXPENSE";

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  date: string;
  needType?: NeedType | null;
}

export type NeedType = "NEED" | "WANT" | "SAVING" | "UNCLASSIFIED";

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  date: string;
}

export async function listTransactions(from?: string, to?: string): Promise<Transaction[]> {
  const params = new URLSearchParams();
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const qs = params.toString();
  return request<Transaction[]>(`/transactions${qs ? `?${qs}` : ""}`);
}

export async function createTransaction(body: TransactionRequest): Promise<Transaction> {
  return request<Transaction>("/transactions", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export async function updateTransaction(id: number, body: TransactionRequest): Promise<Transaction> {
  return request<Transaction>(`/transactions/${id}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

export async function deleteTransaction(id: number): Promise<void> {
  await request<void>(`/transactions/${id}`, { method: "DELETE" });
}

export async function classifyTransaction(
  id: number,
  needType: Exclude<NeedType, "UNCLASSIFIED">
): Promise<Transaction> {
  return request<Transaction>(`/transactions/${id}/classify`, {
    method: "PATCH",
    body: JSON.stringify({ needType }),
    redirectOnAuthError: false
  });
}

export type RegretReviewStatus = "REGRET" | "NO_REGRET" | "NEUTRAL";

export interface PendingRegret {
  id: number;
  description?: string;
  category: string;
  amount: number;
  date: string;
  regretReviewDate: string;
}

export interface CategoryRegretStats {
  category: string;
  reviewed: number;
  regret: number;
  regretRate: number;
}

export interface RegretStats {
  totalReviewed: number;
  totalRegret: number;
  totalNoRegret: number;
  totalNeutral: number;
  regretRate: number;
  mostRegrettedCategory: string;
  mostValuedCategory: string;
  regretByCategory: CategoryRegretStats[];
  totalMoneyRegretted: number;
  averageRegrettedAmount: number;
  recentInsight: string;
}

export async function getPendingRegrets(): Promise<PendingRegret[]> {
  return request<PendingRegret[]>("/transactions/regret/pending", {
    redirectOnAuthError: false
  });
}

export async function submitRegretReview(
  id: number,
  status: RegretReviewStatus
): Promise<Transaction> {
  return request<Transaction>(`/transactions/${id}/regret`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
    redirectOnAuthError: false
  });
}

export async function getRegretStats(): Promise<RegretStats> {
  return request<RegretStats>("/transactions/regret/stats", {
    redirectOnAuthError: false
  });
}

export type SplitType = "I_PAID" | "THEY_PAID";
export type SplitStatus = "PENDING" | "REMINDED" | "SETTLED";

export interface BillSplit {
  id: string;
  title: string;
  totalAmount: number;
  myShare: number;
  otherPersonName: string;
  otherPersonAmount: number;
  splitType: SplitType;
  status: SplitStatus;
  category: string;
  date: string;
  settledDate?: string | null;
  notes?: string | null;
  daysAgo: number;
  overdue: boolean;
}

export interface BillSplitRequest {
  title: string;
  totalAmount: number;
  myShare: number;
  otherPersonName: string;
  otherPersonAmount: number;
  splitType: SplitType;
  category: string;
  date: string;
  notes?: string;
}

export interface BillSplitSummary {
  totalOwedToMe: number;
  totalIOwe: number;
  netBalance: number;
  pendingCount: number;
  overdueCount: number;
}

export async function listBillSplits(status?: string): Promise<BillSplit[]> {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const qs = params.toString();
  return request<BillSplit[]>(`/splits${qs ? `?${qs}` : ""}`, {
    redirectOnAuthError: false
  });
}

export async function getBillSplitSummary(): Promise<BillSplitSummary> {
  return request<BillSplitSummary>("/splits/summary", {
    redirectOnAuthError: false
  });
}

export async function createBillSplit(body: BillSplitRequest): Promise<BillSplit> {
  return request<BillSplit>("/splits", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export async function updateBillSplitStatus(
  id: string,
  status: "REMINDED" | "SETTLED"
): Promise<BillSplit> {
  return request<BillSplit>(`/splits/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
    redirectOnAuthError: false
  });
}

export async function deleteBillSplit(id: string): Promise<void> {
  await request<void>(`/splits/${id}`, {
    method: "DELETE",
    redirectOnAuthError: false
  });
}

export async function exportTransactionsCsv(month?: string): Promise<{ blob: Blob; filename: string }> {
  const params = new URLSearchParams();
  if (month) params.set("month", month);
  const qs = params.toString();

  try {
    const res = await apiClient.get<Blob>(`/transactions/export${qs ? `?${qs}` : ""}`, {
      responseType: "blob"
    });

    const disposition = res.headers["content-disposition"] || "";
    const match = disposition.match(/filename="?([^"]+)"?/i);
    const filename = match?.[1] || (month ? `transactions-${month}.csv` : "transactions.csv");
    return { blob: res.data, filename };
  } catch (error) {
    if (isAxiosError(error)) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        handleUnauthorized();
      }
      throw readAxiosError(error);
    }
    throw error;
  }
}

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  balance: number;
  monthlyIncome?: number;
  topSpendingCategory?: string;
  topSpendingAmount?: number;
  monthlyBudgetLimit?: number;
  monthlyExpenses?: number;
  nearBudgetLimit?: boolean;
  month?: string;
}

export async function getDashboard(): Promise<DashboardSummary> {
  return request<DashboardSummary>("/analytics/dashboard");
}

export type StreakType = "UNDER_BUDGET" | "LOGGING" | "SAVING" | "BILL_PAYMENT";

export interface StreakMetric {
  type: StreakType;
  label: string;
  current: number;
  best: number;
  unit: string;
  broken: boolean;
  brokenMessage?: string | null;
  atRisk: boolean;
}

export interface LoggingDay {
  date: string;
  logged: boolean;
  count: number;
}

export interface StreaksResponse {
  streaks: StreakMetric[];
  loggingHeatmap: LoggingDay[];
}

export async function getStreaks(): Promise<StreaksResponse> {
  return request<StreaksResponse>("/streaks", { redirectOnAuthError: false });
}

export interface Anomaly {
  transactionId: number;
  amount: number;
  category: string;
  date: string;
  reason: string;
}

export async function getAnomalies(): Promise<Anomaly[]> {
  return request<Anomaly[]>("/analytics/anomalies");
}

export interface MonthlyTrend {
  month: string;
  income: number;
  expenses: number;
}

export async function getTrends(months = 6): Promise<MonthlyTrend[]> {
  return request<MonthlyTrend[]>(`/dashboard/trends?months=${months}`);
}

export interface CategoryBreakdown {
  category: string;
  amount: number;
  percentage: number;
}

export async function getCategoryBreakdown(month: string): Promise<CategoryBreakdown[]> {
  return request<CategoryBreakdown[]>(`/dashboard/categories?month=${encodeURIComponent(month)}`);
}

export interface Budget {
  id: number;
  month: string;
  monthlyLimit: number;
}

export async function upsertBudget(month: string, monthlyLimit: number): Promise<Budget> {
  return request<Budget>("/budgets", {
    method: "POST",
    body: JSON.stringify({ month, monthlyLimit })
  });
}

export async function getBudget(month: string): Promise<Budget | null> {
  try {
    const res = await apiClient.get<Budget>(`/budgets/${month}`);
    return res.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null;
    }
    if (isAxiosError(error)) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        handleUnauthorized();
      }
      throw readAxiosError(error);
    }
    throw error;
  }
}

export interface CategoryBudgetItem {
  id: number;
  month: string;
  category: string;
  plannedAmount: number;
  spentAmount: number;
  remainingAmount: number;
  dueDate?: string | null;
  description?: string | null;
  fixed: boolean;
  paid: boolean;
}

export interface CategoryBudgetItemRequest {
  month: string;
  category: string;
  plannedAmount: number;
  dueDate?: string | null;
  description?: string;
  fixed: boolean;
  paid: boolean;
}

export async function listBudgetItems(month: string): Promise<CategoryBudgetItem[]> {
  return request<CategoryBudgetItem[]>(`/budgets/${encodeURIComponent(month)}/items`);
}

export async function createBudgetItem(
  body: CategoryBudgetItemRequest
): Promise<CategoryBudgetItem> {
  return request<CategoryBudgetItem>("/budgets/items", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export async function updateBudgetItem(
  id: number,
  body: CategoryBudgetItemRequest
): Promise<CategoryBudgetItem> {
  return request<CategoryBudgetItem>(`/budgets/items/${id}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

export async function setBudgetItemPaid(
  id: number,
  paid: boolean
): Promise<CategoryBudgetItem> {
  return request<CategoryBudgetItem>(`/budgets/items/${id}/paid`, {
    method: "PUT",
    body: JSON.stringify({ paid })
  });
}

export async function setBudgetCategorySpent(
  month: string,
  category: string,
  spentAmount: number
): Promise<CategoryBudgetItem[]> {
  return request<CategoryBudgetItem[]>("/budgets/spent", {
    method: "PUT",
    body: JSON.stringify({ month, category, spentAmount }),
    redirectOnAuthError: false
  });
}

export async function deleteBudgetItem(id: number): Promise<void> {
  await request<void>(`/budgets/items/${id}`, { method: "DELETE" });
}

export interface CategoryScore {
  category: string;
  score: number;
  max: number;
  explanation: string;
  details?: string[];
}

export interface HealthScore {
  score: number;
  rating: string;
  monthDelta?: number | null;
  breakdown: CategoryScore[];
  positives: string[];
  negatives: string[];
  recommendations: string[];
}

export async function getHealthScore(month?: string): Promise<HealthScore> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : "";
  return request<HealthScore>(`/health-score${qs}`);
}

export interface PurchaseDecision {
  decision: "BUY" | "WAIT" | "NOT_RECOMMENDED" | string;
  confidence: number;
  reason: string;
  alternatives: string[];
}

export async function evaluatePurchase(
  price: number,
  label?: string
): Promise<PurchaseDecision> {
  return request<PurchaseDecision>("/health-score/evaluate-purchase", {
    method: "POST",
    body: JSON.stringify({ price, label })
  });
}

export interface FinancialProfile {
  id?: number;
  userId?: number;
  emergencyFundBalance: number;
  monthlyDebtPayments: number;
  totalDebtOutstanding: number;
  currentBalance?: number;
  salaryDayOfMonth?: number;
}

export async function getFinancialProfile(): Promise<FinancialProfile> {
  return request<FinancialProfile>("/health-inputs/profile");
}

export async function saveFinancialProfile(
  body: Partial<FinancialProfile>
): Promise<FinancialProfile> {
  return request<FinancialProfile>("/health-inputs/profile", {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

export interface SavingsGoal {
  id: number;
  name: string;
  targetAmount: number;
  currentAmount: number;
  deadline: string;
}

export async function listGoals(): Promise<SavingsGoal[]> {
  return request<SavingsGoal[]>("/health-inputs/goals");
}

export async function createGoal(body: {
  name: string;
  targetAmount: number;
  currentAmount: number;
  deadline: string;
}): Promise<SavingsGoal> {
  return request<SavingsGoal>("/health-inputs/goals", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export async function deleteGoal(id: number): Promise<void> {
  await request<void>(`/health-inputs/goals/${id}`, { method: "DELETE" });
}

export type GoalType =
  | "EMERGENCY_FUND"
  | "VACATION"
  | "BIG_PURCHASE"
  | "DEBT_PAYOFF"
  | "CUSTOM";

export interface GoalMilestone {
  percent: number;
  achieved: boolean;
  message?: string | null;
}

export interface FinancialGoal {
  id: number;
  name: string;
  goalType: GoalType;
  targetAmount: number;
  currentAmount: number;
  progressPercent: number;
  targetDate?: string | null;
  linkedCategory?: string | null;
  milestones: GoalMilestone[];
  projectedCompletionDate?: string | null;
  monthlyContributionNeeded?: number | null;
  monthsAheadBehind?: number | null;
  scheduleMessage?: string | null;
  insight?: string | null;
  newlyAchievedMilestones?: GoalMilestone[];
}

export interface EmergencyFundRecommendation {
  averageMonthlyExpenses: number;
  recommendedTarget: number;
  message: string;
}

export async function listFinancialGoals(): Promise<FinancialGoal[]> {
  return request<FinancialGoal[]>("/goals", { redirectOnAuthError: false });
}

export async function getEmergencyFundRecommendation(): Promise<EmergencyFundRecommendation> {
  return request<EmergencyFundRecommendation>("/goals/emergency-recommendation", {
    redirectOnAuthError: false
  });
}

export async function createFinancialGoal(body: {
  name: string;
  goalType: GoalType;
  targetAmount: number;
  currentAmount?: number;
  targetDate?: string;
  linkedCategory?: string;
}): Promise<FinancialGoal> {
  return request<FinancialGoal>("/goals", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export async function contributeToGoal(id: number, amount: number, note?: string): Promise<FinancialGoal> {
  return request<FinancialGoal>(`/goals/${id}/contribute`, {
    method: "POST",
    body: JSON.stringify({ amount, note }),
    redirectOnAuthError: false
  });
}

export async function deleteFinancialGoal(id: number): Promise<void> {
  await request<void>(`/goals/${id}`, { method: "DELETE", redirectOnAuthError: false });
}

export interface RecurringTransaction {
  id: number;
  name: string;
  amount: number;
  type: TransactionType;
  category: string;
  dayOfMonth: number;
  active?: boolean;
}

export interface RecurringTransactionRequest {
  name: string;
  amount: number;
  type: TransactionType;
  category: string;
  dayOfMonth: number;
}

export async function listRecurring(): Promise<RecurringTransaction[]> {
  return request<RecurringTransaction[]>("/recurring", {
    // Don't wipe the session if this endpoint isn't deployed yet
    redirectOnAuthError: false
  });
}

export async function createRecurring(
  body: RecurringTransactionRequest
): Promise<RecurringTransaction> {
  return request<RecurringTransaction>("/recurring", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export async function updateRecurring(
  id: number,
  body: RecurringTransactionRequest
): Promise<RecurringTransaction> {
  return request<RecurringTransaction>(`/recurring/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export async function deleteRecurring(id: number): Promise<void> {
  await request<void>(`/recurring/${id}`, {
    method: "DELETE",
    redirectOnAuthError: false
  });
}

export interface SubscriptionItem {
  id: number;
  name: string;
  monthlyAmount: number;
  unused: boolean;
  duplicate: boolean;
}

export async function listSubscriptions(): Promise<SubscriptionItem[]> {
  return request<SubscriptionItem[]>("/health-inputs/subscriptions");
}

export async function createSubscription(body: {
  name: string;
  monthlyAmount: number;
  unused?: boolean;
  duplicate?: boolean;
}): Promise<SubscriptionItem> {
  return request<SubscriptionItem>("/health-inputs/subscriptions", {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export interface CategoryPrediction {
  category: string;
  budget: number;
  currentSpend: number;
  predictedSpend: number;
  overspendAmount: number;
  riskScorePercent: number;
  riskLevel: string;
  dailySpendRate: number;
  recommendation?: string;
}

export interface BudgetPrediction {
  expectedMonthlySpend: number;
  totalBudget: number;
  expectedSavings: number;
  overspendProbability: number;
  riskLevel: string;
  categories: CategoryPrediction[];
}

export interface ImpactAnalysis {
  balanceAfterPurchase: number;
  remainingBudgetAfter: number;
  emergencyFundMonthsAfter: number;
  savingsGoalDelayDays: number;
  upcomingBillsCovered: boolean;
  expectedEndOfMonthBalance: number;
  budgetUtilizationBeforePercent: number;
  budgetUtilizationAfterPercent: number;
  componentScores: Record<string, number>;
}

export interface PurchaseDecisionDetail {
  affordabilityScore: number;
  decision: string;
  confidence: number;
  explanation: string;
  reason: string;
  recommendedPurchaseDate?: string;
  requiredSavings?: number;
  estimatedDaysToAfford?: number;
  impactAnalysis?: ImpactAnalysis;
  alternatives: string[];
}

export interface FinancialAdvisory {
  prediction: BudgetPrediction;
  purchaseDecision?: PurchaseDecisionDetail;
  recommendations: string[];
  simulations?: SimulationResult[];
}

export interface SimulationResult {
  scenarioName: string;
  healthScore?: number;
  expectedMonthlySpend?: number;
  expectedSavings?: number;
  emergencyFundMonths?: number;
  remainingBudget?: number;
  goalCompletionDate?: string;
  purchaseAffordabilityScore?: number;
  purchaseDecision?: string;
}

export async function getFinancialAdvisory(month?: string): Promise<FinancialAdvisory> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : "";
  return request<FinancialAdvisory>(`/health-score/advisory${qs}`, { redirectOnAuthError: false });
}

export async function evaluatePurchaseDetailed(body: {
  price: number;
  label?: string;
  category?: string;
  priority?: "NECESSITY" | "WANT" | "LUXURY" | string;
}): Promise<FinancialAdvisory> {
  return request<FinancialAdvisory>("/health-score/evaluate-purchase", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export async function runSimulations(
  scenarios: {
    scenarioName?: string;
    salaryIncrease?: number;
    rentIncrease?: number;
    postponePurchaseDays?: number;
    purchaseAmount?: number;
    purchaseCategory?: string;
    categorySpendAdjustments?: Record<string, number>;
  }[],
  month?: string
): Promise<FinancialAdvisory> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : "";
  return request<FinancialAdvisory>(`/health-score/simulate${qs}`, {
    method: "POST",
    body: JSON.stringify(scenarios),
    redirectOnAuthError: false
  });
}

export interface CashFlowDay {
  date: string;
  projectedBalance: number;
  events: string[];
}

export interface CashFlowForecast {
  startingBalance: number;
  days: CashFlowDay[];
  lowestPoint: { date: string | null; balance: number } | null;
  willGoNegative: boolean;
  negativeDates: string[];
  summary?: string;
}

export async function getCashFlowForecast(month?: string): Promise<CashFlowForecast> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : "";
  return request<CashFlowForecast>(`/forecast/cashflow${qs}`, {
    redirectOnAuthError: false
  });
}

export interface HealthNarrative {
  narrative: string;
  tone: "positive" | "warning" | "critical" | string;
}

export async function getHealthNarrative(month?: string): Promise<HealthNarrative> {
  const qs = month ? `?month=${encodeURIComponent(month)}` : "";
  return request<HealthNarrative>(`/health/narrative${qs}`, {
    redirectOnAuthError: false
  });
}

export interface PurchaseSimulation {
  canAfford: boolean;
  affordabilityScore: number;
  immediateImpact: {
    newBalance: number;
    balanceChange: number;
    percentOfBalance: number;
  };
  budgetImpact: {
    categoryBudget: number;
    categorySpent: number;
    afterPurchase: number;
    willExceedBudget: boolean;
    exceedBy?: number;
  };
  healthScoreImpact: {
    currentScore: number;
    projectedScore: number;
    change: number;
    reason: string;
  };
  savingsImpact: {
    currentMonthlySavings: number;
    afterPurchaseSavings: number;
    currentSavingsRate?: number;
    afterSavingsRate?: number;
    savingsRateChange: number;
  };
  verdict: "GO_AHEAD" | "CONSIDER" | "AVOID" | string;
  verdictReason: string;
  alternatives: string[];
}

export async function simulatePurchase(body: {
  itemName: string;
  price: number;
  category: string;
  priority: "NEED" | "WANT" | "INVESTMENT" | string;
  paymentDate?: string;
}): Promise<PurchaseSimulation> {
  return request<PurchaseSimulation>("/simulate/purchase", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export interface TradeoffOptionInput {
  name: string;
  amount: number;
  type: "PURCHASE" | "SAVING" | string;
}

export interface TradeoffOptionResult {
  name: string;
  type?: string;
  immediateBalanceImpact: number;
  monthlyImpact: number;
  healthScoreImpact: number;
  timeToRecover: string;
  pros: string[];
  cons: string[];
}

export interface TradeoffComparison {
  option1: TradeoffOptionResult;
  option2: TradeoffOptionResult;
  recommendation: "option1" | "option2" | string;
  recommendationReason: string;
}

export async function compareTradeoff(body: {
  option1: TradeoffOptionInput;
  option2: TradeoffOptionInput;
}): Promise<TradeoffComparison> {
  return request<TradeoffComparison>("/simulate/tradeoff", {
    method: "POST",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export interface DailyInsight {
  type: "TIP" | "WARNING" | "ACHIEVEMENT" | "REMINDER" | string;
  insight: string;
  action?: string | null;
  actionRoute?: string | null;
}

export async function getDailyInsight(): Promise<DailyInsight> {
  return request<DailyInsight>("/advisor/daily", {
    redirectOnAuthError: false
  });
}

export interface SalaryProfile {
  salaryDay?: number | null;
  salaryAmount?: number | null;
  configured: boolean;
}

export async function saveSalaryProfile(body: {
  salaryDay: number;
  salaryAmount: number;
}): Promise<SalaryProfile> {
  return request<SalaryProfile>("/profile/salary", {
    method: "PUT",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}

export interface AllocationBreakdownItem {
  label: string;
  amount: number;
  kind?: "fixed" | "savings" | "discretionary" | string;
}

export interface AllocationPlan {
  salaryAmount: number;
  fixedCosts: number;
  suggestedSavings: number;
  discretionary: number;
  breakdown: AllocationBreakdownItem[];
}

export interface SalaryIntelligence {
  configured: boolean;
  zone?: "PRE_SALARY" | "POST_SALARY" | string;
  daysUntilSalary?: number;
  salaryDay?: number;
  salaryAmount?: number;
  currentBalance?: number;
  dailyBudget?: number;
  showAllocation?: boolean;
  allocationPlan?: AllocationPlan | null;
  daysInMonth?: number;
  todayDayOfMonth?: number;
  monthLabel?: string;
}

export async function getSalaryIntelligence(): Promise<SalaryIntelligence> {
  return request<SalaryIntelligence>("/forecast/salary-intelligence", {
    redirectOnAuthError: false
  });
}

export interface CategoryAmount {
  category: string;
  amount: number;
}

export interface BucketAnalysis {
  amount: number;
  percent: number;
  idealPercent: number;
  diff: number;
  status: "ON_TRACK" | "OVER" | "UNDER" | string;
  insight: string;
  topCategories?: CategoryAmount[];
}

export interface UnclassifiedTransaction {
  id: number;
  description?: string | null;
  category: string;
  amount: number;
}

export interface Rule502030Analysis {
  month: string;
  totalIncome: number;
  needs: BucketAnalysis;
  wants: BucketAnalysis;
  savings: BucketAnalysis;
  unclassifiedAmount: number;
  unclassifiedTransactions: UnclassifiedTransaction[];
  overallStatus: "HEALTHY" | "NEEDS_ATTENTION" | "CRITICAL" | string;
  overallInsight: string;
}

export async function get502030Analysis(month: string): Promise<Rule502030Analysis> {
  return request<Rule502030Analysis>(
    `/analyzer/502030?month=${encodeURIComponent(month)}`,
    { redirectOnAuthError: false }
  );
}

export interface NamedAmount {
  id?: number;
  name: string;
  amount: number;
}

export interface NetWorthBreakdownItem {
  label: string;
  amount: number;
  percent: number;
}

export interface NetWorthHistoryPoint {
  month: string;
  netWorth: number;
  totalAssets: number;
  totalLiabilities: number;
}

export interface NetWorthData {
  useAutoBankBalance: boolean;
  autoBankBalance: number;
  bankBalance: number;
  fixedDeposits: number;
  investments: number;
  physicalAssets: NamedAmount[];
  studentLoan: number;
  creditCardDebt: number;
  moneyOwed: NamedAmount[];
  totalAssets: number;
  totalLiabilities: number;
  netWorth: number;
  liquidAssets: number;
  fixedAssets: number;
  liquidPercent: number;
  fixedPercent: number;
  assetBreakdown: NetWorthBreakdownItem[];
  liabilityBreakdown: NetWorthBreakdownItem[];
  monthOverMonthChange: number;
  monthOverMonthMessage: string;
  history: NetWorthHistoryPoint[];
}

export interface NetWorthUpdateRequest {
  useAutoBankBalance: boolean;
  bankBalance: number;
  fixedDeposits: number;
  investments: number;
  physicalAssets: NamedAmount[];
  studentLoan: number;
  creditCardDebt: number;
  moneyOwed: NamedAmount[];
}

export async function getNetWorth(): Promise<NetWorthData> {
  return request<NetWorthData>("/net-worth", { redirectOnAuthError: false });
}

export async function saveNetWorth(body: NetWorthUpdateRequest): Promise<NetWorthData> {
  return request<NetWorthData>("/net-worth", {
    method: "PUT",
    body: JSON.stringify(body),
    redirectOnAuthError: false
  });
}
