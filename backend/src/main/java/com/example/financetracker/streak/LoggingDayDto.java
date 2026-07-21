package com.example.financetracker.streak;

import java.time.LocalDate;

public class LoggingDayDto {

    private LocalDate date;
    private boolean logged;
    private int count;

    public LoggingDayDto() {
    }

    public LoggingDayDto(LocalDate date, boolean logged, int count) {
        this.date = date;
        this.logged = logged;
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
