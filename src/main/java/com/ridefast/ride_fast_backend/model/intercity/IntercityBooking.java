package com.ridefast.ride_fast_backend.model.intercity;

import com.ridefast.ride_fast_backend.enums.IntercityBookingStatus;
import com.ridefast.ride_fast_backend.enums.IntercityBookingType;
import com.ridefast.ride_fast_backend.enums.IntercityPaymentMethod;
import com.ridefast.ride_fast_backend.enums.PaymentStatus;
import com.ridefast.ride_fast_backend.model.MyUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main booking entity for intercity rides
 * Can be PRIVATE (full vehicle) or SHARE_POOL (seat-based)
 */
@Entity
@Table(name = "intercity_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique booking reference code */
    @Column(nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private MyUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private IntercityTrip trip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntercityBookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IntercityBookingStatus status = IntercityBookingStatus.PENDING;

    /** Number of seats booked (1 for share pool, all for private) */
    @Column(nullable = false)
    private Integer seatsBooked;

    /** Total amount for this booking */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** Amount per seat at time of booking */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal perSeatAmount;

    /** Payment status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /** Payment method */
    @Enumerated(EnumType.STRING)
    private IntercityPaymentMethod paymentMethod;

    /** Razorpay order ID */
    private String razorpayOrderId;

    /** Razorpay payment ID */
    private String razorpayPaymentId;
    
    /** Commission amount deducted (5% of total amount) */
    @Column(precision = 10, scale = 2)
    private BigDecimal commissionAmount;
    
    /** Whether commission has been deducted */
    @Builder.Default
    private Boolean commissionDeducted = false;
    
    /** Time when commission was deducted */
    private LocalDateTime commissionDeductedAt;

    /** Contact phone for booking */
    private String contactPhone;

    /** Special instructions */
    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    /** OTP for ride verification */
    private Integer otp;

    /** OTP verified flag - true when driver verifies passenger */
    @Builder.Default
    private Boolean otpVerified = false;

    /** Time when OTP was verified */
    private LocalDateTime otpVerifiedAt;

    /** Number of passengers actually onboarded (verified by driver) */
    @Builder.Default
    private Integer passengersOnboarded = 0;

    /** Refund amount (if applicable) */
    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /** Refund reason */
    private String refundReason;

    /** Seat bookings associated with this booking */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IntercitySeatBooking> seatBookings = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (otp == null) otp = generateOtp();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private Integer generateOtp() {
        return 1000 + (int) (Math.random() * 9000);
    }

    /**
     * Check if booking is cancellable
     */
    public boolean isCancellable() {
        return status == IntercityBookingStatus.PENDING || 
               status == IntercityBookingStatus.CONFIRMED;
    }
}

