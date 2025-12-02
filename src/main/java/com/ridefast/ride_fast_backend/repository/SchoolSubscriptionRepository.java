package com.ridefast.ride_fast_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ridefast.ride_fast_backend.model.school.SchoolSubscription;

@Repository
public interface SchoolSubscriptionRepository extends JpaRepository<SchoolSubscription, Long> {
    List<SchoolSubscription> findByInstitutionId(Long institutionId);

    Optional<SchoolSubscription> findByInstitutionIdAndStatus(Long institutionId, String status);
}
