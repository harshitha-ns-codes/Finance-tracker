package com.example.financetracker.networth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class NetWorthSnapshotScheduler {

    private final NetWorthService netWorthService;

    public NetWorthSnapshotScheduler(NetWorthService netWorthService) {
        this.netWorthService = netWorthService;
    }

    /** Runs at 2:00 AM on the 1st of each month. */
    @Scheduled(cron = "0 0 2 1 * *")
    public void captureMonthlySnapshots() {
        netWorthService.snapshotAllUsersForMonth(YearMonth.now());
    }
}
