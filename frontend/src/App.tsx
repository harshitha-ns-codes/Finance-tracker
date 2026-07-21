import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { Suspense, lazy, useEffect, useState } from "react";
import { getToken } from "./auth";
import { TopNav } from "./components/TopNav";
import { SplashScreen } from "./components/SplashScreen";

const LoginPage = lazy(() =>
  import("./pages/LoginPage").then(m => ({ default: m.LoginPage }))
);
const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then(m => ({ default: m.DashboardPage }))
);
const TransactionsPage = lazy(() =>
  import("./pages/TransactionsPage").then(m => ({ default: m.TransactionsPage }))
);
const BudgetPage = lazy(() =>
  import("./pages/BudgetPage").then(m => ({ default: m.BudgetPage }))
);
const ForecastPage = lazy(() =>
  import("./pages/ForecastPage").then(m => ({ default: m.ForecastPage }))
);
const ProfilePage = lazy(() =>
  import("./pages/ProfilePage").then(m => ({ default: m.ProfilePage }))
);
const HealthScorePage = lazy(() =>
  import("./pages/HealthScorePage").then(m => ({ default: m.HealthScorePage }))
);

const SPLASH_MS = 4000;
const SPLASH_SEEN_KEY = "finbrain_splash_seen";

function hasSeenSplash(): boolean {
  try {
    return sessionStorage.getItem(SPLASH_SEEN_KEY) === "true";
  } catch {
    return false;
  }
}

function markSplashSeen(): void {
  try {
    sessionStorage.setItem(SPLASH_SEEN_KEY, "true");
  } catch {
    // ignore
  }
}

function PrivateRoute({ children }: { children: JSX.Element }) {
  const token = getToken();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function PageLoader() {
  return <div className="page">Loading...</div>;
}

function shouldShowSplash(): boolean {
  if (hasSeenSplash()) return false;
  if (typeof window !== "undefined" && window.location.pathname === "/login") {
    return false;
  }
  return true;
}

export default function App() {
  const [showSplash, setShowSplash] = useState(() => shouldShowSplash());
  const navigate = useNavigate();
  const location = useLocation();
  const isLoginPage = location.pathname === "/login";

  useEffect(() => {
    if (!showSplash) return;

    const timer = window.setTimeout(() => {
      markSplashSeen();
      setShowSplash(false);
      navigate(getToken() ? "/" : "/login", { replace: true });
    }, SPLASH_MS);

    return () => window.clearTimeout(timer);
  }, [showSplash, navigate]);

  if (showSplash) {
    return <SplashScreen />;
  }

  return (
    <div className="app-shell">
      {!isLoginPage && <TopNav />}
      <main className="app-main">
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <PrivateRoute>
                  <DashboardPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/transactions"
              element={
                <PrivateRoute>
                  <TransactionsPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/plan"
              element={
                <PrivateRoute>
                  <BudgetPage />
                </PrivateRoute>
              }
            />
            <Route path="/budget" element={<Navigate to="/plan" replace />} />
            <Route
              path="/forecast"
              element={
                <PrivateRoute>
                  <ForecastPage />
                </PrivateRoute>
              }
            />
            <Route
              path="/advisor"
              element={
                <PrivateRoute>
                  <HealthScorePage />
                </PrivateRoute>
              }
            />
            <Route path="/health" element={<Navigate to="/advisor" replace />} />
            <Route
              path="/profile"
              element={
                <PrivateRoute>
                  <ProfilePage />
                </PrivateRoute>
              }
            />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}
