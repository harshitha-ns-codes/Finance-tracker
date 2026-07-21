package com.example.financetracker.split;

import com.example.financetracker.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillSplitRepository extends JpaRepository<BillSplit, UUID> {

    List<BillSplit> findByUserOrderByDateDescIdDesc(User user);

    List<BillSplit> findByUserAndStatusOrderByDateDescIdDesc(User user, SplitStatus status);

    List<BillSplit> findByUserAndStatusInOrderByDateDescIdDesc(User user, List<SplitStatus> statuses);

    Optional<BillSplit> findByIdAndUser(UUID id, User user);
}
