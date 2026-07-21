package com.example.financetracker.streak;

import java.util.ArrayList;
import java.util.List;

public class StreaksResponse {

    private List<StreakMetricDto> streaks = new ArrayList<>();
    private List<LoggingDayDto> loggingHeatmap = new ArrayList<>();

    public List<StreakMetricDto> getStreaks() {
        return streaks;
    }

    public void setStreaks(List<StreakMetricDto> streaks) {
        this.streaks = streaks;
    }

    public List<LoggingDayDto> getLoggingHeatmap() {
        return loggingHeatmap;
    }

    public void setLoggingHeatmap(List<LoggingDayDto> loggingHeatmap) {
        this.loggingHeatmap = loggingHeatmap;
    }
}
