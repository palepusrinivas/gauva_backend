package com.ridefast.ride_fast_backend.service.parcel.impl;

import com.ridefast.ride_fast_backend.dto.parcel.AddParcelRefundProofRequest;
import com.ridefast.ride_fast_backend.dto.parcel.CreateParcelRefundRequest;
import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefund;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefundProof;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.repository.UserRepository;
import com.ridefast.ride_fast_backend.repository.parcel.ParcelRefundProofRepository;
import com.ridefast.ride_fast_backend.repository.parcel.ParcelRefundRepository;
import com.ridefast.ride_fast_backend.service.parcel.ParcelRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParcelRefundServiceImpl implements ParcelRefundService {

    private final ParcelRefundRepository refundRepository;
    private final ParcelRefundProofRepository proofRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ParcelRefund createRefund(CreateParcelRefundRequest req) {
        Ride ride = rideRepository.findById(req.getRideId()).orElseThrow();
        MyUser user = userRepository.findById(String.valueOf(req.getUserId())).orElseThrow();
        ParcelRefund r = ParcelRefund.builder()
                .ride(ride)
                .user(user)
                .reason(req.getReason())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        return refundRepository.save(r);
    }

    @Override
    public ParcelRefund getRefund(Long id) {
        return refundRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public ParcelRefundProof addProof(Long refundId, AddParcelRefundProofRequest req) {
        ParcelRefund refund = refundRepository.findById(refundId).orElseThrow();
        ParcelRefundProof p = ParcelRefundProof.builder()
                .refund(refund)
                .imageUrl(req.getImageUrl())
                .build();
        return proofRepository.save(p);
    }
}


