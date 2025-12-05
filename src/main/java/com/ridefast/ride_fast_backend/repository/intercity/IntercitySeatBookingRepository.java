package com.ridefast.ride_fast_backend.repository.intercity;

import com.ridefast.ride_fast_backend.enums.IntercitySeatStatus;
import com.ridefast.ride_fast_backend.model.intercity.IntercitySeatBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IntercitySeatBookingRepository extends JpaRepository<IntercitySeatBooking, Long> {
    
    List<IntercitySeatBooking> findByTripId(Long tripId);
    
    List<IntercitySeatBooking> findByTripIdAndStatus(Long tripId, IntercitySeatStatus status);
    
    List<IntercitySeatBooking> findByBookingId(Long bookingId);
    
    List<IntercitySeatBooking> findByUserId(String userId);
    
    /**
     * Find next available seat number for a trip
     */
    @Query("""
        SELECT COALESCE(MAX(s.seatNumber), 0) + 1 
        FROM IntercitySeatBooking s 
        WHERE s.trip.id = :tripId
        """)
    Integer findNextSeatNumber(@Param("tripId") Long tripId);
    
    /**
     * Find expired seat locks
     */
    @Query("""
        SELECT s FROM IntercitySeatBooking s 
        WHERE s.status = :status 
        AND s.lockExpiry < :now
        """)
    List<IntercitySeatBooking> findExpiredLocks(
        @Param("status") IntercitySeatStatus status,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Count booked seats for a trip
     */
    @Query("""
        SELECT COUNT(s) FROM IntercitySeatBooking s 
        WHERE s.trip.id = :tripId 
        AND s.status = :status
        """)
    Integer countBookedSeats(
        @Param("tripId") Long tripId,
        @Param("status") IntercitySeatStatus status
    );
}

