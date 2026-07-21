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

  const isActive = (paths: string | string[]) => {
    const list = Array.isArray(paths) ? paths : [paths];
    return list.includes(location.pathname) ? "active" : "";
  };

  return (
    <header className="top-nav">
      <div className="top-nav-left">
        <Link to="/" className="brand">
          Finbrain
        </Link>
        {authed && (
          <nav className="nav-links">
            <Link className={isActive("/")} to="/">
              Home
            </Link>
            <Link className={isActive("/transactions")} to="/transactions">
              Transactions
            </Link>
            <Link className={isActive(["/plan", "/budget"])} to="/plan">
              Plan
            </Link>
            <Link className={isActive("/forecast")} to="/forecast">
              Forecast
            </Link>
            <Link className={isActive(["/advisor", "/health"])} to="/advisor">
              Advisor
            </Link>
            <Link className={isActive("/profile")} to="/profile">
              Profile
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
