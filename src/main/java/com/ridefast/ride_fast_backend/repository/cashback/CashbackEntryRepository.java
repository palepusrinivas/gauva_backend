package com.ridefast.ride_fast_backend.repository.cashback;

import com.ridefast.ride_fast_backend.enums.CashbackStatus;
import com.ridefast.ride_fast_backend.model.cashback.CashbackEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashbackEntryRepository extends JpaRepository<CashbackEntry, Long> {

    /**
     * Find all active cashback entries for a user (FIFO - oldest first)
     */
    @Query("""
        SELECT c FROM CashbackEntry c 
        WHERE c.userId = :userId 
        AND c.status = 'ACTIVE' 
        AND c.expiresAt > :now
        AND c.amountRemaining > 0
        ORDER BY c.createdAt ASC
        """)
    List<CashbackEntry> findActiveEntriesByUser(
        @Param("userId") String userId,
        @Param("now") LocalDateTime now
    );

    /**
     * Get total active balance for a user
     */
    @Query("""
        SELECT COALESCE(SUM(c.amountRemaining), 0) FROM CashbackEntry c 
        WHERE c.userId = :userId 
        AND c.status = 'ACTIVE' 
        AND c.expiresAt > :now
        """)
    BigDecimal getTotalActiveBalance(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Count credits for user today (for daily limit check)
     */
    @Query("""
        SELECT COUNT(c) FROM CashbackEntry c 
        WHERE c.userId = :userId 
        AND c.createdAt >= :startOfDay
        """)
    int countCreditsToday(@Param("userId") String userId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * Find entries expiring soon (for reminders)
     */
    @Query("""
        SELECT c FROM CashbackEntry c 
        WHERE c.status = 'ACTIVE' 
        AND c.expiresAt BETWEEN :startTime AND :endTime
        AND c.amountRemaining > 0
        """)
    List<CashbackEntry> findEntriesExpiringSoon(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find expired entries that need to be processed
     */
    @Query("""
        SELECT c FROM CashbackEntry c 
        WHERE c.status = 'ACTIVE' 
        AND c.expiresAt < :now
        """)
    List<CashbackEntry> findExpiredEntries(@Param("now") LocalDateTime now);

    /**
     * Get user's cashback history by status
     */
    List<CashbackEntry> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, CashbackStatus status);

    /**
     * Get all user's cashback history
     */
    List<CashbackEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Check if cashback already credited for this ride
     */
    boolean existsByRideId(Long rideId);

    /**
     * Admin: Get all entries with pagination
     */
    Page<CashbackEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Admin: Get entries by status
     */
    Page<CashbackEntry> findByStatusOrderByCreatedAtDesc(CashbackStatus status, Pageable pageable);

    /**
     * Get sum of cashback by status for dashboard
     */
    @Query("""
        SELECT c.status, COUNT(c), COALESCE(SUM(c.amount), 0) 
        FROM CashbackEntry c 
        GROUP BY c.status
        """)
    List<Object[]> getCashbackStatsByStatus();

    /**
     * Get total cashback credited in date range
     */
    @Query("""
        SELECT COALESCE(SUM(c.amount), 0) FROM CashbackEntry c 
        WHERE c.createdAt BETWEEN :startDate AND :endDate
        """)
    BigDecimal getTotalCashbackCredited(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get total cashback used in date range
     */
    @Query("""
        SELECT COALESCE(SUM(c.amountUsed), 0) FROM CashbackEntry c 
        WHERE c.usedAt BETWEEN :startDate AND :endDate
        OR (c.status IN ('USED', 'PARTIALLY_USED') AND c.createdAt BETWEEN :startDate AND :endDate)
        """)
    BigDecimal getTotalCashbackUsed(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

