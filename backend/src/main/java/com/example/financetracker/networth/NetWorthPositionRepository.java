package com.example.financetracker.networth;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface NetWorthPositionRepository extends JpaRepository<NetWorthPosition, Long> {

    List<NetWorthPosition> findByUser(User user);

    Optional<NetWorthPosition> findByUserAndPositionTypeAndId(User user, PositionType positionType, Long id);
}
