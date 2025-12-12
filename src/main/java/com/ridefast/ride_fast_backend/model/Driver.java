package com.ridefast.ride_fast_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private Double rating = 0.0;
    private Double latitude;
    private Double longitude;
    private UserRole role;

    @Column(name = "short_code", unique = true, length = 4)
    private String shortCode;

    private String password;
    @Column(name = "fcm_token")
    private String fcmToken;
    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL)
    private License license;

    @JsonIgnore
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ride> rides = new ArrayList<>();

    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private Vehicle vehicle;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    private Ride currentRide;

    private Long totalRevenue = 0L;

    @JsonIgnore
    @OneToOne(mappedBy = "driver")
    private RefreshToken refreshToken;

    @JsonIgnore
    @OneToOne(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private DriverKyc kyc;

    // Bank and UPI details
    private String accountHolderName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String upiId;
    private String bankAddress;
    private String bankMobile;

    @Enumerated(EnumType.STRING)
    private VerificationStatus bankVerificationStatus = VerificationStatus.PENDING;
    private String bankVerificationNotes;
    private LocalDateTime bankVerifiedAt;

    // Online/Offline status
    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private Boolean isOnline = false;

}
