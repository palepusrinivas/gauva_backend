package com.ridefast.ride_fast_backend.enums;

/**
 * Status of an intercity trip
 */
public enum IntercityTripStatus {
    /** Trip created, waiting for bookings */
    PENDING,
    
    /** Trip scheduled for future departure */
    SCHEDULED,
    
    /** Trip has bookings but min seats not reached */
    FILLING,
    
    /** Minimum seats reached, ready for dispatch */
    MIN_REACHED,
    
    /** Trip dispatched to driver */
    DISPATCHED,
    
    /** Trip is in progress */
    IN_PROGRESS,
    
    /** Trip completed */
    COMPLETED,
    
    /** Trip cancelled */
    CANCELLED,
    
    /** Trip expired (countdown finished, min seats not met) */
    EXPIRED
}

