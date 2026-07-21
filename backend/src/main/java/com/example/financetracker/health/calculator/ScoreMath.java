package com.example.financetracker.health.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ScoreMath {

    private ScoreMath() {}

    /** Linear interpolation of points between zeroAt and fullAt for value. */
    public static int scalePoints(double value, double zeroAt, double fullAt, int maxPoints) {
        if (maxPoints <= 0) return 0;
        if (Double.compare(fullAt, zeroAt) == 0) {
            return value >= fullAt ? maxPoints : 0;
        }
        double ratio;
        if (fullAt > zeroAt) {
            ratio = (value - zeroAt) / (fullAt - zeroAt);
        } else {
            ratio = (zeroAt - value) / (zeroAt - fullAt);
        }
        ratio = Math.max(0, Math.min(1, ratio));
        return (int) Math.round(ratio * maxPoints);
    }

    public static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    public static double asDouble(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    public static BigDecimal safeDiv(BigDecimal num, BigDecimal den) {
        if (den == null || den.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return num.divide(den, 4, RoundingMode.HALF_UP);
    }

    public static String inr(BigDecimal amount) {
        if (amount == null) return "₹0";
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
