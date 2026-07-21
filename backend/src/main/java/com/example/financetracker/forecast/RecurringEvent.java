package com.example.financetracker.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Internal scheduled cash movement for a single calendar day. */
record RecurringEvent(LocalDate date, String label, BigDecimal delta) {
}
