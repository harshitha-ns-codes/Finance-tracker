import { Link, useLocation, useNavigate } from "react-router-dom";
import { clearToken, getToken } from "../auth";

export function TopNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const authed = !!getToken();

  const handleLogout = () => {
    clearToken();
    navigate("/login");
  };

  const isActive = (path: string) => (location.pathname === path ? "active" : "");

  return (
    <header className="top-nav">
      <div className="top-nav-left">
        <span className="brand">AI Finance Tracker</span>
        {authed && (
          <nav className="nav-links">
            <Link className={isActive("/")} to="/">
              Dashboard
            </Link>
            <Link className={isActive("/transactions")} to="/transactions">
              Transactions
            </Link>
            <Link className={isActive("/budget")} to="/budget">
              Budget
            </Link>
          </nav>
        )}
      </div>
      <div className="top-nav-right">
        {authed ? (
          <button className="btn-secondary" onClick={handleLogout}>
            Logout
          </button>
        ) : (
          <Link className="btn-primary" to="/login">
            Login
          </Link>
        )}
      </div>
    </header>
  );
}

