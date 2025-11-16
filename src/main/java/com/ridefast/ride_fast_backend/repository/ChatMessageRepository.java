package com.ridefast.ride_fast_backend.repository;

import com.ridefast.ride_fast_backend.model.ChatMessage;
import com.ridefast.ride_fast_backend.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRideOrderByCreatedAtAsc(Ride ride);
}


