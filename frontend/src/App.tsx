import { Navigate, Route, Routes } from "react-router-dom";
import { useEffect, useState } from "react";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { TransactionsPage } from "./pages/TransactionsPage";
import { BudgetPage } from "./pages/BudgetPage";
import { getToken } from "./auth";
import { TopNav } from "./components/TopNav";

function PrivateRoute({ children }: { children: JSX.Element }) {
  const token = getToken();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setReady(true);
  }, []);

  if (!ready) return null;

  return (
    <div className="app-shell">
      <TopNav />
      <main className="app-main">
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
            path="/budget"
            element={
              <PrivateRoute>
                <BudgetPage />
              </PrivateRoute>
            }
          />
        </Routes>
      </main>
    </div>
  );
}

