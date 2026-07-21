import axios, {
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig
} from "axios";
import { clearToken, getToken } from "../auth";

/** Base URL from Vite env — defaults to /api for dev proxy */
const rawBase = import.meta.env.VITE_API_BASE_URL as string | undefined;
export const API_BASE_URL = (rawBase ?? "/api").replace(/\/$/, "");

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" }
});

export const publicClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" }
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function handleUnauthorized(): void {
  clearToken();
  if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
    window.location.assign("/login");
  }
}

export type ApiErrorBody = {
  error?: string;
  fields?: Record<string, string>;
};

export function readAxiosError(error: AxiosError<ApiErrorBody>): string {
  const status = error.response?.status;
  const body = error.response?.data;

  if (body?.error) {
    return body.error;
  }

  if ((status === 401 || status === 403) && !body?.error) {
    return "Session expired. Please log in again.";
  }

  if (error.message) {
    return error.message;
  }

  return `Request failed with status ${status ?? "unknown"}`;
}

/** Converts fetch-style body JSON string to axios data object */
export function toAxiosConfig(
  options: RequestInit & { redirectOnAuthError?: boolean } = {}
): AxiosRequestConfig & { redirectOnAuthError?: boolean } {
  const { body, headers, redirectOnAuthError, method } = options;

  const config: AxiosRequestConfig & { redirectOnAuthError?: boolean } = {
    method: (method as AxiosRequestConfig["method"]) ?? "GET",
    headers: headers as AxiosRequestConfig["headers"],
    redirectOnAuthError
  };

  if (body && typeof body === "string") {
    try {
      config.data = JSON.parse(body);
    } catch {
      config.data = body;
    }
  }

  return config;
}

export function isAxiosError(error: unknown): error is AxiosError<ApiErrorBody> {
  return axios.isAxiosError(error);
}
