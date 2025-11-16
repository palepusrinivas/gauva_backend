package com.ridefast.ride_fast_backend.controller.parcel;

import com.ridefast.ride_fast_backend.dto.parcel.AddParcelRefundProofRequest;
import com.ridefast.ride_fast_backend.dto.parcel.CreateParcelRefundRequest;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefund;
import com.ridefast.ride_fast_backend.model.parcel.ParcelRefundProof;
import com.ridefast.ride_fast_backend.service.parcel.ParcelRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parcel/refunds")
@RequiredArgsConstructor
public class ParcelRefundController {

    private final ParcelRefundService refundService;

    @PostMapping
    public ResponseEntity<ParcelRefund> create(@RequestBody @Validated CreateParcelRefundRequest req) {
        return new ResponseEntity<>(refundService.createRefund(req), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParcelRefund> get(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.getRefund(id));
    }

    @PostMapping("/{id}/proofs")
    public ResponseEntity<ParcelRefundProof> addProof(@PathVariable Long id, @RequestBody @Validated AddParcelRefundProofRequest req) {
        return new ResponseEntity<>(refundService.addProof(id, req), HttpStatus.CREATED);
    }
}


