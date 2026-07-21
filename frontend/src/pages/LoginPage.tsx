import { FormEvent, useEffect, useState } from "react";
import { checkApiReachable, login, register } from "../api";
import { ApiError } from "../api/client";
import { setToken } from "../auth";
import { useNavigate } from "react-router-dom";

export function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [apiUnreachable, setApiUnreachable] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;

    checkApiReachable().then(ok => {
      if (!cancelled) {
        setApiUnreachable(!ok);
      }
    });

    return () => {
      cancelled = true;
    };
  }, []);

  const switchMode = (next: "login" | "register") => {
    setMode(next);
    setError(null);
    setFieldErrors({});
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);

    try {
      const result =
        mode === "login"
          ? await login(username, password)
          : await register(username, email, password);
      setToken(result.token);
      navigate("/", { replace: true });
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        setError(err.message);
        if (err.fields) {
          setFieldErrors(err.fields);
        }
      } else if (err instanceof Error) {
        setError(err.message || "Something went wrong");
      } else {
        setError("Something went wrong");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="centered-card">
      <div className="card auth-card">
        <p className="brand-login">Finbrain</p>
        <h1>{mode === "login" ? "Welcome back" : "Create your account"}</h1>
        <p className="subtitle">
          {mode === "login"
            ? "Sign in to your financial brain."
            : "Sign up to plan, track, and get advice in one place."}
        </p>

        {apiUnreachable && (
          <div className="error" role="alert">
            Cannot reach the API server.
            {import.meta.env.PROD
              ? " Set VITE_API_BASE_URL in Vercel to your Render backend (e.g. https://your-api.onrender.com/api)."
              : " Start the backend: cd backend && .\\mvnw.cmd spring-boot:run"}
          </div>
        )}

        <div className="tabs">
          <button
            type="button"
            className={mode === "login" ? "tab active" : "tab"}
            onClick={() => switchMode("login")}
          >
            Login
          </button>
          <button
            type="button"
            className={mode === "register" ? "tab active" : "tab"}
            onClick={() => switchMode("register")}
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
              minLength={3}
              spellCheck={false}
              autoCorrect="off"
              autoCapitalize="off"
              autoComplete="username"
            />
            {fieldErrors.username && (
              <span className="field-error">{fieldErrors.username}</span>
            )}
          </label>
          {mode === "register" && (
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                spellCheck={false}
                autoComplete="email"
              />
              {fieldErrors.email && (
                <span className="field-error">{fieldErrors.email}</span>
              )}
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
              autoComplete={mode === "login" ? "current-password" : "new-password"}
            />
            {fieldErrors.password && (
              <span className="field-error">{fieldErrors.password}</span>
            )}
          </label>
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          <button className="btn-primary full-width" type="submit" disabled={loading}>
            {loading ? "Please wait..." : mode === "login" ? "Login" : "Register"}
          </button>
        </form>
      </div>
    </div>
  );
}
