package com.ridefast.ride_fast_backend.dto.intercity;

import com.ridefast.ride_fast_backend.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for intercity booking details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercityBookingResponse {
    private Long bookingId;
    private String bookingCode;
    
    /** Booking details */
    private IntercityBookingType bookingType;
    private IntercityBookingStatus bookingStatus;
    private Integer seatsBooked;
    private BigDecimal totalAmount;
    private BigDecimal perSeatAmount;
    private Integer otp;
    
    /** OTP verification status */
    private Boolean otpVerified;
    private LocalDateTime otpVerifiedAt;
    private Integer passengersOnboarded;
    
    /** Payment details */
    private PaymentStatus paymentStatus;
    private String razorpayOrderId;
    
    /** Trip details */
    private TripInfo trip;
    
    /** Seat details */
    private List<SeatInfo> seats;
    
    /** Timestamps */
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripInfo {
        private Long tripId;
        private String tripCode;
        private IntercityVehicleType vehicleType;
        private String vehicleDisplayName;
        private IntercityTripStatus tripStatus;
        
        private String pickupAddress;
        private Double pickupLatitude;
        private Double pickupLongitude;
        
        private String dropAddress;
        private Double dropLatitude;
        private Double dropLongitude;
        
        private LocalDateTime scheduledDeparture;
        private LocalDateTime countdownExpiry;
        
        private Integer totalSeats;
        private Integer seatsBooked;
        private Integer availableSeats;
        private Integer minSeats;
        private Boolean minSeatsMet;
        private Integer passengersOnboarded;
        
        private BigDecimal totalPrice;
        private BigDecimal currentPerHeadPrice;
        
        /** Driver info (if assigned) */
        private DriverInfo driver;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverInfo {
        private Long driverId;
        private String name;
        private String phone;
        private String vehicleNumber;
        private String vehicleModel;
        private Double rating;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SeatInfo {
        private Integer seatNumber;
        private IntercitySeatStatus status;
        private BigDecimal pricePaid;
        private String passengerName;
        private String passengerPhone;
    }
}

