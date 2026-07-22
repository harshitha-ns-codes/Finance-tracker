import { FormEvent, useEffect, useState } from "react";
import { checkApiHealth, login, register } from "../api";
import { API_BASE_URL, ApiError } from "../api/client";
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
  const [apiDiag, setApiDiag] = useState<{ url: string; detail?: string } | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;

    checkApiHealth().then(result => {
      if (!cancelled) {
        setApiUnreachable(!result.ok);
        if (!result.ok) {
          setApiDiag({ url: result.url, detail: result.detail });
        }
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
            <strong>Cannot reach the API server.</strong>
            <p style={{ margin: "0.5rem 0 0" }}>
              Calling: <code>{apiDiag?.url ?? `${API_BASE_URL}/health`}</code>
            </p>
            {apiDiag?.detail && (
              <p style={{ margin: "0.35rem 0 0" }}>Reason: {apiDiag.detail}</p>
            )}
            {import.meta.env.PROD ? (
              <p style={{ margin: "0.5rem 0 0" }}>
                Fix checklist: (1) Render backend must be <strong>Live</strong> — open{" "}
                <code>https://finance-tracker-evut.onrender.com/api/health</code> in a new tab;
                (2) On Vercel set <code>VITE_API_BASE_URL=/api</code> (proxy) or your full Render
                URL + <code>/api</code>; (3) redeploy Vercel after env changes; (4) on Render set{" "}
                <code>CORS_ALLOWED_ORIGINS</code> to your Vercel URL if not using the proxy.
              </p>
            ) : (
              <p style={{ margin: "0.5rem 0 0" }}>
                Start the backend: <code>cd backend &amp;&amp; .\mvnw.cmd spring-boot:run</code>
              </p>
            )}
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
