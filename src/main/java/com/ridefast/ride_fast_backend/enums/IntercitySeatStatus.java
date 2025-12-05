package com.ridefast.ride_fast_backend.enums;

/**
 * Status of a seat in an intercity trip
 */
public enum IntercitySeatStatus {
    /** Seat is available for booking */
    AVAILABLE,
    
    /** Seat is temporarily locked (payment pending) */
    LOCKED,
    
    /** Seat is confirmed/booked */
    BOOKED,
    
    /** Seat booking was cancelled */
    CANCELLED
}

