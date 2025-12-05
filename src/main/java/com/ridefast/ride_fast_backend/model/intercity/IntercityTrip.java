package com.ridefast.ride_fast_backend.model.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityTripStatus;
import com.ridefast.ride_fast_backend.enums.IntercityVehicleType;
import com.ridefast.ride_fast_backend.model.Driver;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an intercity trip with a specific vehicle on a route
 * Multiple passengers can book seats on this trip (for SHARE_POOL)
 */
@Entity
@Table(name = "intercity_trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityTrip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique trip code for display */
    @Column(nullable = false, unique = true)
    private String tripCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private IntercityRoute route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntercityVehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_config_id")
    private IntercityVehicleConfig vehicleConfig;

    /** Assigned driver (null until dispatched) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IntercityTripStatus status = IntercityTripStatus.PENDING;

    /** Total seats available for this trip */
    @Column(nullable = false)
    private Integer totalSeats;

    /** Number of seats currently booked */
    @Column(nullable = false)
    @Builder.Default
    private Integer seatsBooked = 0;

    /** Number of passengers actually onboarded (verified via OTP) */
    @Column(nullable = false)
    @Builder.Default
    private Integer passengersOnboarded = 0;

    /** Minimum seats required to dispatch */
    @Column(nullable = false)
    private Integer minSeats;

    /** Total vehicle price */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Current per-head price (recalculated on each booking) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPerHeadPrice;

    /** Scheduled departure time */
    @Column(nullable = false)
    private LocalDateTime scheduledDeparture;

    /** Countdown expiry time (when to check if min seats met) */
    private LocalDateTime countdownExpiry;

    /** Actual departure time */
    private LocalDateTime actualDeparture;

    /** Actual arrival time */
    private LocalDateTime actualArrival;

    /** Origin details (can override route) */
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;

    /** Destination details (can override route) */
    private String dropAddress;
    private Double dropLatitude;
    private Double dropLongitude;

    /** Whether this is a private booking (full vehicle) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    /** Seat bookings for this trip */
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IntercitySeatBooking> seatBookings = new ArrayList<>();

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
     * Check if minimum seats requirement is met
     */
    public boolean isMinSeatsMet() {
        return seatsBooked >= minSeats;
    }

    /**
     * Get available seats count
     */
    public int getAvailableSeats() {
        return totalSeats - seatsBooked;
    }

    /**
     * Recalculate per-head price based on current bookings
     */
    public void recalculatePerHeadPrice() {
        if (seatsBooked > 0) {
            this.currentPerHeadPrice = totalPrice.divide(
                BigDecimal.valueOf(seatsBooked), 2, java.math.RoundingMode.CEILING
            );
        } else {
            this.currentPerHeadPrice = totalPrice;
        }
    }
}

