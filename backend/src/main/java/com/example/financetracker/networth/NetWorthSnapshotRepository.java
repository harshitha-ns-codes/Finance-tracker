package com.example.financetracker.networth;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Long> {

    List<NetWorthSnapshot> findByUserOrderBySnapshotMonthAsc(User user);

    Optional<NetWorthSnapshot> findByUserAndSnapshotMonth(User user, YearMonth snapshotMonth);
}
