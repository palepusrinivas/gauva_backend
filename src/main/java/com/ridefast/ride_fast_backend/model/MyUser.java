package com.ridefast.ride_fast_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ridefast.ride_fast_backend.enums.UserRole;
import com.ridefast.ride_fast_backend.enums.AuthProvider;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "MyUser")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "password")
    private String password;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "user_level_id")
    private UUID userLevelId;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "loyalty_points")
    private Double loyaltyPoints;

    @Column(name = "failed_attempts")
    private Integer failedAttempt;

    @Column(name = "is_temp_blocked")
    private Boolean isTempBlocked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "identification_type")
    private String identificationType;

    @Column(name = "identification_image")
    private String identificationImage;

    @Column(name = "old_identification_image")
    private String oldIdentificationImage;

    @Column(name = "other_documents")
    private String otherDocuments;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "ref_code")
    private String refCode;

    @Column(name = "ref_by")
    private String refBy;

    @Column(name = "user_type")
    private String userType;

    @Column(name = "remember_token")
    private String rememberToken;

    @Column(name = "current_language_key")
    private String currentLanguageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "firebase_uid")
    private String firebaseUid;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "current_ride_id")
    private Ride currentRide;

    @OneToOne(mappedBy = "user")
    private RefreshToken refreshToken;
}
