package com.ridefast.ride_fast_backend.enums;

/**
 * Payment method for intercity bookings
 */
public enum IntercityPaymentMethod {
    /** Cash payment - commission deducted at end of day */
    CASH,
    
    /** UPI payment - instant commission deduction */
    UPI,
    
    /** Wallet payment - instant commission deduction */
    WALLET,
    
    /** Razorpay online payment */
    ONLINE
}

