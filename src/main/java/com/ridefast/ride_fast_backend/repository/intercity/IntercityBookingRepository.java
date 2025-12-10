package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityBookingStatus;
import com.ridefast.ride_fast_backend.model.intercity.IntercityBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntercityBookingRepository extends JpaRepository<IntercityBooking, Long> {
    
    Optional<IntercityBooking> findByBookingCode(String bookingCode);
    
    boolean existsByBookingCode(String bookingCode);
    
    /**
     * Find bookings by user
     */
    List<IntercityBooking> findByUserIdOrderByCreatedAtDesc(String oderId);
    
    /**
     * Find bookings by user with trip and driver eagerly loaded
     */
    @Query("""
        SELECT DISTINCT b FROM IntercityBooking b 
        LEFT JOIN FETCH b.trip t 
        LEFT JOIN FETCH t.driver 
        WHERE b.user.id = :userId 
        ORDER BY b.createdAt DESC
        """)
    List<IntercityBooking> findByUserIdWithTripAndDriver(@Param("userId") String userId);
    
    /**
     * Find all bookings with trip and driver eagerly loaded (for driver view)
     */
    @Query("""
        SELECT DISTINCT b FROM IntercityBooking b 
        LEFT JOIN FETCH b.trip t 
        LEFT JOIN FETCH t.driver 
        ORDER BY b.createdAt DESC
        """)
    List<IntercityBooking> findAllWithTripAndDriver();
    
    /**
     * Find bookings by user and status
     */
    List<IntercityBooking> findByUserIdAndStatusIn(String userId, List<IntercityBookingStatus> statuses);
    
    /**
     * Find bookings by trip
     */
    List<IntercityBooking> findByTripId(Long tripId);
    
    /**
     * Find active bookings for a user (not completed/cancelled)
     */
    @Query("""
        SELECT b FROM IntercityBooking b 
        WHERE b.user.id = :userId 
        AND b.status NOT IN ('COMPLETED', 'CANCELLED', 'REFUNDED')
        ORDER BY b.createdAt DESC
        """)
    List<IntercityBooking> findActiveBookingsByUser(@Param("userId") String userId);
    
    /**
     * Find bookings pending payment beyond timeout
     */
    @Query("""
        SELECT b FROM IntercityBooking b 
        WHERE b.status = 'PENDING' 
        AND b.paymentStatus = 'PENDING'
        AND b.createdAt < :timeout
        """)
    List<IntercityBooking> findPendingBookingsBeyondTimeout(@Param("timeout") LocalDateTime timeout);
    
    /**
     * Admin: paginated bookings with filters
     */
    @Query("""
        SELECT b FROM IntercityBooking b 
        WHERE (:status IS NULL OR b.status = :status)
        ORDER BY b.createdAt DESC
        """)
    Page<IntercityBooking> findAllWithFilters(
        @Param("status") IntercityBookingStatus status,
        Pageable pageable
    );
    
    /**
     * Count bookings by status for admin dashboard
     */
    @Query("SELECT b.status, COUNT(b) FROM IntercityBooking b GROUP BY b.status")
    List<Object[]> countByStatus();
    
    /**
     * Revenue summary
     */
    @Query("""
        SELECT SUM(b.totalAmount) FROM IntercityBooking b 
        WHERE b.status = 'COMPLETED' 
        AND b.createdAt BETWEEN :startDate AND :endDate
        """)
    java.math.BigDecimal getTotalRevenue(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find bookings by trip and status
     */
    List<IntercityBooking> findByTripIdAndStatusIn(Long tripId, List<IntercityBookingStatus> statuses);

    /**
     * Count bookings by trip and status
     */
    int countByTripIdAndStatusIn(Long tripId, List<IntercityBookingStatus> statuses);

    /**
     * Sum of passengers onboarded for a trip
     */
    @Query("""
        SELECT COALESCE(SUM(b.passengersOnboarded), 0) FROM IntercityBooking b 
        WHERE b.trip.id = :tripId 
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        """)
    int sumPassengersOnboardedByTripId(@Param("tripId") Long tripId);

    /**
     * Count pending OTP verifications for a trip
     */
    @Query("""
        SELECT COUNT(b) FROM IntercityBooking b 
        WHERE b.trip.id = :tripId 
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND (b.otpVerified IS NULL OR b.otpVerified = false)
        """)
    int countPendingVerificationsByTripId(@Param("tripId") Long tripId);

    /**
     * Count verified bookings for a trip
     */
    @Query("""
        SELECT COUNT(b) FROM IntercityBooking b 
        WHERE b.trip.id = :tripId 
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.otpVerified = true
        """)
    int countVerifiedByTripId(@Param("tripId") Long tripId);
    
    /**
     * Find bookings by trip, status, and created after date (for auto-confirm)
     */
    @Query("""
        SELECT b FROM IntercityBooking b 
        WHERE b.trip.id = :tripId 
        AND b.status = :status
        AND b.createdAt >= :createdAfter
        """)
    List<IntercityBooking> findByTripIdAndStatusAndCreatedAtAfter(
        @Param("tripId") Long tripId,
        @Param("status") IntercityBookingStatus status,
        @Param("createdAfter") LocalDateTime createdAfter
    );
}

