import { getToken } from "./auth";

const BASE_URL = "/api";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`;
    try {
      const body = await res.json();
      if (body.error) {
        message = body.error;
      }
    } catch {
      // ignore
    }
    throw new Error(message);
  }

  if (res.status === 204) {
    return {} as T;
  }

  return (await res.json()) as T;
}

export async function login(username: string, password: string) {
  return request<{ token: string }>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  });
}

export async function register(username: string, email: string, password: string) {
  return request<{ token: string }>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password })
  });
}

export type TransactionType = "INCOME" | "EXPENSE";

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  date: string;
}

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  date: string;
}

export async function listTransactions(): Promise<Transaction[]> {
  return request<Transaction[]>("/transactions");
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

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  balance: number;
  topSpendingCategory?: string;
  topSpendingAmount?: number;
  monthlyBudgetLimit?: number;
  monthlyExpenses?: number;
  nearBudgetLimit?: boolean;
}

export async function getDashboard(): Promise<DashboardSummary> {
  return request<DashboardSummary>("/analytics/dashboard");
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

export async function getBudget(month: string): Promise<Budget> {
  return request<Budget>(`/budgets/${month}`);
}

