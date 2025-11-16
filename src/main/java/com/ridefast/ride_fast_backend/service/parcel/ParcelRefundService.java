package com.ridefast.ride_fast_backend.service.parcel;

import com.ridefast.ride_fast_backend.dto.parcel.AddParcelRefundProofRequest;
import com.ridefast.ride_fast_backend.dto.parcel.CreateParcelRefundRequest;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefund;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefundProof;

public interface ParcelRefundService {
    ParcelRefund createRefund(CreateParcelRefundRequest req);
    ParcelRefund getRefund(Long id);
    ParcelRefundProof addProof(Long refundId, AddParcelRefundProofRequest req);
}


