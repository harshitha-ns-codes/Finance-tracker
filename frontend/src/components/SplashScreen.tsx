export function SplashScreen() {
  return (
    <div className="splash-screen" role="status" aria-label="Loading Finbrain">
      <div className="splash-content">
        <div className="splash-illustration" aria-hidden>
          <svg viewBox="0 0 320 320" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle className="splash-blob" cx="160" cy="150" r="118" fill="var(--beige-300)" />

            <g className="splash-coin splash-coin-1">
              <circle cx="78" cy="88" r="26" fill="var(--gold-500)" stroke="#2c1810" strokeWidth="3" />
              <circle cx="78" cy="88" r="16" fill="none" stroke="#2c1810" strokeWidth="2.5" />
            </g>
            <g className="splash-coin splash-coin-2">
              <circle cx="108" cy="62" r="20" fill="var(--gold-400)" stroke="#2c1810" strokeWidth="3" />
              <circle cx="108" cy="62" r="12" fill="none" stroke="#2c1810" strokeWidth="2" />
            </g>

            <path
              className="splash-sparkle splash-sparkle-1"
              d="M248 72l4 10 10 4-10 4-4 10-4-10-10-4 10-4 4-10z"
              fill="var(--gold-500)"
            />
            <path
              className="splash-sparkle splash-sparkle-2"
              d="M58 188l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z"
              fill="var(--maroon-600)"
            />
            <path
              className="splash-sparkle splash-sparkle-3"
              d="M92 228l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z"
              fill="var(--gold-500)"
            />
            <path
              className="splash-sparkle splash-sparkle-4"
              d="M268 168l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z"
              fill="var(--maroon-500)"
            />

            <g className="splash-hand-group">
              <path
                d="M118 248c8-18 22-34 42-44 10-5 22-8 34-6 6 1 10 6 9 12-1 8-8 14-16 16-14 4-28 12-38 24-6 7-12 16-16 26-3 8-10 12-18 10-10-2-16-12-15-22z"
                fill="#f0d5c4"
                stroke="#2c1810"
                strokeWidth="3"
                strokeLinejoin="round"
              />
              <path
                d="M148 188c18-8 36-4 50 8 8 7 12 18 10 29-2 14-12 24-26 28-18 5-36-2-48-16"
                fill="#f0d5c4"
                stroke="#2c1810"
                strokeWidth="3"
                strokeLinejoin="round"
              />
              <rect
                x="154"
                y="118"
                width="118"
                height="78"
                rx="10"
                transform="rotate(-18 213 157)"
                fill="var(--maroon-700)"
                stroke="#2c1810"
                strokeWidth="3"
              />
              <rect
                x="162"
                y="132"
                width="102"
                height="18"
                rx="2"
                transform="rotate(-18 213 141)"
                fill="#2c1810"
              />
              <rect
                x="236"
                y="146"
                width="28"
                height="20"
                rx="3"
                transform="rotate(-18 250 156)"
                fill="var(--bg-card)"
                stroke="#2c1810"
                strokeWidth="2"
              />
              <path
                d="M176 176c8 4 18 6 28 4"
                stroke="var(--gold-300)"
                strokeWidth="2.5"
                strokeLinecap="round"
                transform="rotate(-18 190 178)"
              />
            </g>
          </svg>
        </div>

        <div className="splash-brand-block">
          <h1 className="splash-brand">Finbrain</h1>
          <p className="splash-tagline">Your financial brain</p>
        </div>

        <div className="splash-loader" aria-hidden>
          <span />
          <span />
          <span />
        </div>
      </div>
    </div>
  );
}
