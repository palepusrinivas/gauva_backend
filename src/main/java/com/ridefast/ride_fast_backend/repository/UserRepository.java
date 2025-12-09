package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.model.MyUser;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<MyUser, String> {
    public Optional<MyUser> findByEmail(String email);

    boolean existsByShortCode(String shortCode);

    @Query("select r from Ride r where r.status=COMPLETED and r.user.id=:userId")
    public List<Ride> getCompletedRides(
            @Param("userId") String userId);

    @Query("select r from Ride r where r.status=ACCEPTED or r.status=STARTED and r.user.id=:userId")
    public List<Ride> getCurrentRides(@Param("userId") String userId);

    @Query("select r from Ride r where r.status=REQUESTED and r.user.id=:userId")
    public List<Ride> getRequestedRides(@Param("userId") String userId);

    // New lookups for phone-based auth
    Optional<MyUser> findByPhone(String phone);

    @Query("select u from MyUser u where u.email = :identifier or u.phone = :identifier")
    Optional<MyUser> findByEmailOrPhone(@Param("identifier") String identifier);

    // Search methods - includes ID, fullName, email, and phone
    @Query("select u from MyUser u where " +
           "LOWER(u.id) LIKE '%' || LOWER(:search) || '%' OR " +
           "(u.fullName IS NOT NULL AND LOWER(u.fullName) LIKE '%' || LOWER(:search) || '%') OR " +
           "(u.email IS NOT NULL AND LOWER(u.email) LIKE '%' || LOWER(:search) || '%') OR " +
           "(u.phone IS NOT NULL AND u.phone LIKE '%' || :search || '%')")
    org.springframework.data.domain.Page<MyUser> searchUsers(@Param("search") String search, org.springframework.data.domain.Pageable pageable);
}
