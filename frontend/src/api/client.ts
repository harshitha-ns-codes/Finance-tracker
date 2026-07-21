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

export class ApiError extends Error {
  status?: number;
  fields?: Record<string, string>;
  isNetworkError?: boolean;

  constructor(
    message: string,
    options: { status?: number; fields?: Record<string, string>; isNetworkError?: boolean } = {}
  ) {
    super(message);
    this.name = "ApiError";
    this.status = options.status;
    this.fields = options.fields;
    this.isNetworkError = options.isNetworkError;
  }
}

function isHtmlResponse(data: unknown): boolean {
  return typeof data === "string" && data.trimStart().startsWith("<!");
}

export function readAxiosError(
  error: AxiosError<ApiErrorBody>,
  options: { authRequest?: boolean } = {}
): ApiError {
  const status = error.response?.status;
  const body = error.response?.data;

  if (isHtmlResponse(body)) {
    return new ApiError(
      import.meta.env.PROD
        ? "API misconfigured: set VITE_API_BASE_URL to your Render backend URL (including /api)."
        : "Backend returned HTML instead of JSON. Start the API on port 8081 or check the Vite proxy.",
      { status, isNetworkError: true }
    );
  }

  if (body?.fields && Object.keys(body.fields).length > 0) {
    const detail = Object.values(body.fields).join(" ");
    return new ApiError(body.error ?? detail, { status, fields: body.fields });
  }

  if (body?.error) {
    return new ApiError(body.error, { status });
  }

  if (status === 401 && options.authRequest) {
    return new ApiError("Invalid username or password.", { status });
  }

  if ((status === 401 || status === 403) && !options.authRequest) {
    return new ApiError("Session expired. Please log in again.", { status });
  }

  if (error.code === "ERR_NETWORK" || error.message === "Network Error") {
    return new ApiError(
      import.meta.env.PROD
        ? "Cannot reach the API server. Check VITE_API_BASE_URL and that the backend is running."
        : "Cannot reach the API server. Start the backend (port 8081) with: cd backend && .\\mvnw.cmd spring-boot:run",
      { isNetworkError: true }
    );
  }

  if (status === 404) {
    return new ApiError(
      import.meta.env.PROD
        ? "API endpoint not found. VITE_API_BASE_URL should end with /api (no trailing slash)."
        : "API endpoint not found. Is the backend running on port 8081?",
      { status }
    );
  }

  if (error.message) {
    return new ApiError(error.message, { status });
  }

  return new ApiError(`Request failed with status ${status ?? "unknown"}`, { status });
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
