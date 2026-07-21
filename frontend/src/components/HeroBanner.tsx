import { useEffect, useRef, useState } from "react";

const HEADLINES = [
  {
    heading: "Your Wallet Called.\nWe Listened.",
    sub: `Because "I'll start budgeting next month" has been going on for a while.`
  },
  {
    heading: "Money Makes Decisions.\nWe Prefer You Do.",
    sub: "See tomorrow's consequences before today's purchases."
  },
  {
    heading: "Your Future Self\nAlready Has Opinions.",
    sub: "We just make them easier to hear."
  },
  {
    heading: "Turns Out, Math\nCan Save Money.",
    sub: "And no, you won't need a calculator."
  },
  {
    heading: "Making Bad Financial\nDecisions Slightly Harder.",
    sub: "Because online shopping is already doing enough."
  },
  {
    heading: "Spend Less on Regret.",
    sub: "More on things you'll actually remember."
  },
  {
    heading: "Your Money Deserves\nBetter Excuses.",
    sub: "We'll help with that."
  },
  {
    heading: "Built for \u201cShould I\nBuy This?\u201d Moments.",
    sub: "Before your cart becomes tomorrow's life lesson."
  },
  {
    heading: "Helping Your Salary\nSurvive the Month.",
    sub: "One smarter decision at a time."
  },
  {
    heading: "Because \u201cIt\u2019s Just \u20b9499\u201d\nAdds Up.",
    sub: `See the real cost before you click "Buy Now."`
  },
  {
    heading: "Financial Advice.\nMinus the Lecture.",
    sub: "Just smart insights that make sense."
  }
];

const HERO_IDX_KEY = "finbrain_hero_idx";

function getHeadlineIndex(): number {
  try {
    const stored = sessionStorage.getItem(HERO_IDX_KEY);
    if (stored !== null) {
      const idx = Number(stored);
      if (idx >= 0 && idx < HEADLINES.length) return idx;
    }
  } catch {
    // ignore
  }

  const next = Math.floor(Math.random() * HEADLINES.length);
  try {
    sessionStorage.setItem(HERO_IDX_KEY, String(next));
  } catch {
    // ignore
  }
  return next;
}

export function HeroBanner() {
  const [idx] = useState(getHeadlineIndex);
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<number>();

  useEffect(() => {
    timerRef.current = window.setTimeout(() => setVisible(true), 60);
    return () => window.clearTimeout(timerRef.current);
  }, []);

  const { heading, sub } = HEADLINES[idx];

  return (
    <section className={`hero-full ${visible ? "hero-full--visible" : ""}`}>
      {/* Background decorative circles */}
      <span className="hero-deco hero-deco-1" aria-hidden />
      <span className="hero-deco hero-deco-2" aria-hidden />

      <div className="hero-inner">
        {/* LEFT — text */}
        <div className="hero-text">
          <h2 className="hero-heading">
            {heading.split("\n").map((line, i) => (
              <span key={i} className="hero-heading-line">{line}</span>
            ))}
          </h2>
          <p className="hero-sub">{sub}</p>
          <p className="hero-scroll-hint" aria-hidden>↓ scroll to see your numbers</p>
        </div>

        {/* RIGHT — SVG illustration */}
        <div className="hero-illustration" aria-hidden>
          <svg viewBox="0 0 420 360" fill="none" xmlns="http://www.w3.org/2000/svg">
            {/* background blob */}
            <ellipse className="hb-blob" cx="270" cy="190" rx="148" ry="142" fill="var(--beige-300)" />

            {/* ── scale base ── */}
            <rect x="246" y="272" width="20" height="42" rx="5" fill="var(--maroon-800)" stroke="#2c1810" strokeWidth="2.5" />
            <ellipse cx="256" cy="316" rx="48" ry="11" fill="var(--maroon-700)" stroke="#2c1810" strokeWidth="2.5" />

            {/* ── beam ── */}
            <line x1="130" y1="168" x2="382" y2="168" stroke="#2c1810" strokeWidth="4.5" strokeLinecap="round" />
            <circle cx="256" cy="162" r="12" fill="var(--gold-500)" stroke="#2c1810" strokeWidth="3" />

            {/* ── left pan & strings ── */}
            <line x1="142" y1="170" x2="130" y2="232" stroke="#2c1810" strokeWidth="2.5" />
            <ellipse cx="130" cy="234" rx="42" ry="9" fill="var(--maroon-600)" stroke="#2c1810" strokeWidth="2.5" />

            {/* ── person on left pan ── */}
            <g className="hb-person">
              {/* body */}
              <rect x="108" y="185" width="44" height="42" rx="11" fill="var(--maroon-700)" stroke="#2c1810" strokeWidth="2.5" />
              {/* head */}
              <circle cx="130" cy="172" r="19" fill="#f0d5c4" stroke="#2c1810" strokeWidth="2.5" />
              {/* hair */}
              <path d="M112 166c3-14 34-14 36 0" fill="#2c1218" />
              {/* legs */}
              <path d="M112 227 l-13 24" stroke="#2c1810" strokeWidth="3.5" strokeLinecap="round" />
              <path d="M148 227 l10 24" stroke="#2c1810" strokeWidth="3.5" strokeLinecap="round" />
              {/* shoes */}
              <ellipse cx="97" cy="253" rx="10" ry="5" fill="#2c1218" />
              <ellipse cx="160" cy="253" rx="10" ry="5" fill="#2c1218" />
              {/* laptop base */}
              <rect x="108" y="204" width="44" height="26" rx="4" fill="var(--maroon-600)" stroke="#2c1810" strokeWidth="2" />
              <rect x="110" y="206" width="40" height="20" rx="3" fill="var(--gold-300)" />
              {/* screen content lines */}
              <rect x="113" y="209" width="34" height="3" rx="1.5" fill="var(--maroon-800)" opacity="0.4" />
              <rect x="113" y="214" width="24" height="3" rx="1.5" fill="var(--maroon-800)" opacity="0.3" />
              <rect x="113" y="219" width="28" height="3" rx="1.5" fill="var(--maroon-800)" opacity="0.3" />
            </g>

            {/* ── right pan & strings ── */}
            <line x1="370" y1="170" x2="382" y2="232" stroke="#2c1810" strokeWidth="2.5" />
            <ellipse cx="382" cy="234" rx="42" ry="9" fill="var(--maroon-600)" stroke="#2c1810" strokeWidth="2.5" />

            {/* ── money bag ── */}
            <g className="hb-bag">
              <ellipse cx="382" cy="210" rx="30" ry="28" fill="var(--gold-400)" stroke="#2c1810" strokeWidth="3" />
              <path d="M368 186 q14-18 28 0" fill="none" stroke="#2c1810" strokeWidth="2.5" strokeLinecap="round" />
              <text x="382" y="216" textAnchor="middle" fontSize="22" fill="var(--maroon-800)" fontWeight="800">₹</text>
              {/* plant */}
              <path d="M374 181 c-4-12 4-20 12-14" stroke="var(--color-positive)" strokeWidth="2.5" fill="none" strokeLinecap="round" />
              <path d="M382 178 c0-12 12-16 14-8" stroke="var(--color-positive)" strokeWidth="2.5" fill="none" strokeLinecap="round" />
              <circle cx="370" cy="168" r="7" fill="var(--color-positive)" stroke="#2c1810" strokeWidth="2" />
              <circle cx="392" cy="171" r="6" fill="var(--color-positive)" stroke="#2c1810" strokeWidth="2" />
            </g>

            {/* ── floating coins ── */}
            <g className="hb-coin hb-coin-1">
              <circle cx="68" cy="168" r="24" fill="var(--gold-500)" stroke="#2c1810" strokeWidth="3" />
              <circle cx="68" cy="168" r="14" fill="none" stroke="#2c1810" strokeWidth="2" />
            </g>
            <g className="hb-coin hb-coin-2">
              <circle cx="102" cy="118" r="17" fill="var(--gold-400)" stroke="#2c1810" strokeWidth="2.5" />
              <circle cx="102" cy="118" r="10" fill="none" stroke="#2c1810" strokeWidth="1.5" />
            </g>

            {/* ── sparkles ── */}
            <path className="hb-sparkle hb-sparkle-1" d="M44 108 l4 10 10 4-10 4-4 10-4-10-10-4 10-4 4-10z" fill="var(--gold-500)" />
            <path className="hb-sparkle hb-sparkle-2" d="M390 88 l3.5 8.5 8.5 3.5-8.5 3.5-3.5 8.5-3.5-8.5-8.5-3.5 8.5-3.5 3.5-8.5z" fill="var(--maroon-500)" />
            <path className="hb-sparkle hb-sparkle-3" d="M44 272 l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z" fill="var(--maroon-600)" />
            <path className="hb-sparkle hb-sparkle-4" d="M410 260 l3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7z" fill="var(--gold-400)" />
          </svg>
        </div>
      </div>
    </section>
  );
}
