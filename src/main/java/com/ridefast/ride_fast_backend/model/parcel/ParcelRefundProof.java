package com.ridefast.ride_fast_backend.model.parcel;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parcel_refund_proofs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParcelRefundProof {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ParcelRefund refund;

    @Column(nullable = false, length = 512)
    private String imageUrl;
}


