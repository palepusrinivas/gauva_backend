package com.ridefast.ride_fast_backend.model.parcel;

import com.ridefast.ride_fast_backend.model.MyUser;
import com.ridefast.ride_fast_backend.model.Ride;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parcel_refunds")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParcelRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Ride ride;

    @ManyToOne(optional = false)
    private MyUser user;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false, length = 32)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParcelRefundProof> proofs = new ArrayList<>();
}


