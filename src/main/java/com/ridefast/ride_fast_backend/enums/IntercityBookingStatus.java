package com.ridefast.ride_fast_backend.enums;

/**
 * Status of an intercity booking
 */
public enum IntercityBookingStatus {
    /** Booking created, payment pending */
    PENDING,
    
    /** Payment completed, waiting for trip dispatch */
    CONFIRMED,
    
    /** Trip is in progress */
    IN_PROGRESS,
    
    /** Booking completed */
    COMPLETED,
    
    /** Booking cancelled by user */
    CANCELLED,
    
    /** Refund issued (trip not filled, user declined alternatives) */
    REFUNDED
}

