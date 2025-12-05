package com.ridefast.ride_fast_backend.model.intercity;

import com.ridefast.ride_fast_backend.enums.IntercitySeatStatus;
import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Individual seat booking within an intercity trip
 */
@Entity
@Table(name = "intercity_seat_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercitySeatBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private IntercityTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private IntercityBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MyUser user;

    /** Seat number (1, 2, 3, etc.) */
    @Column(nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IntercitySeatStatus status = IntercitySeatStatus.LOCKED;

    /** Price paid for this seat at time of booking */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    /** Passenger name for this seat */
    private String passengerName;

    /** Passenger phone for this seat */
    private String passengerPhone;

    /** Lock expiry time (for payment pending) */
    private LocalDateTime lockExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if seat lock has expired
     */
    public boolean isLockExpired() {
        return lockExpiry != null && LocalDateTime.now().isAfter(lockExpiry);
    }
}

