import { FormEvent, useState } from "react";
import { login, register } from "../api";
import { setToken } from "../auth";
import { useNavigate } from "react-router-dom";

export function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const result =
        mode === "login"
          ? await login(username, password)
          : await register(username, email, password);
      setToken(result.token);
      navigate("/");
    } catch (err: any) {
      setError(err.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="centered-card">
      <div className="card auth-card">
        <h1>{mode === "login" ? "Welcome back" : "Create your account"}</h1>
        <p className="subtitle">
          {mode === "login"
            ? "Sign in to track your spending and income."
            : "Sign up to start managing your personal finances."}
        </p>
        <div className="tabs">
          <button
            className={mode === "login" ? "tab active" : "tab"}
            onClick={() => setMode("login")}
          >
            Login
          </button>
          <button
            className={mode === "register" ? "tab active" : "tab"}
            onClick={() => setMode("register")}
          >
            Register
          </button>
        </div>
        <form onSubmit={handleSubmit} className="form">
          <label>
            Username
            <input
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
            />
          </label>
          {mode === "register" && (
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
            </label>
          )}
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              minLength={6}
            />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="btn-primary full-width" type="submit" disabled={loading}>
            {loading ? "Please wait..." : mode === "login" ? "Login" : "Register"}
          </button>
        </form>
      </div>
    </div>
  );
}

