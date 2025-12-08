package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.RideDto;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.Ride;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Service for broadcasting real-time updates via WebSocket
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Broadcast ride status update to user and driver
   * Topics:
   * - /topic/ride/{rideId}/status (for both user and driver)
   * - /topic/user/{userId}/rides (for user's ride list updates)
   * - /topic/driver/{driverId}/rides (for driver's ride list updates)
   */
  public void broadcastRideStatusUpdate(Ride ride, RideDto rideDto) {
    if (ride == null || ride.getId() == null) {
      log.warn("Cannot broadcast ride update: ride or rideId is null");
      return;
    }

    String rideTopic = "/topic/ride/" + ride.getId() + "/status";
    
    try {
      // Broadcast to ride-specific topic (both user and driver can subscribe)
      messagingTemplate.convertAndSend(rideTopic, rideDto);
      log.debug("Broadcasted ride status update to: {}", rideTopic);

      // Broadcast to user-specific topic
      if (ride.getUser() != null && ride.getUser().getId() != null) {
        String userTopic = "/topic/user/" + ride.getUser().getId() + "/rides";
        messagingTemplate.convertAndSend(userTopic, rideDto);
        log.debug("Broadcasted ride update to user topic: {}", userTopic);
      }

      // Broadcast to driver-specific topic
      if (ride.getDriver() != null && ride.getDriver().getId() != null) {
        String driverTopic = "/topic/driver/" + ride.getDriver().getId() + "/rides";
        messagingTemplate.convertAndSend(driverTopic, rideDto);
        log.debug("Broadcasted ride update to driver topic: {}", driverTopic);
      }
    } catch (Exception e) {
      log.error("Error broadcasting ride status update for ride {}: {}", ride.getId(), e.getMessage(), e);
    }
  }

  /**
   * Broadcast driver status update (online/offline)
   * Topic: /topic/driver/{driverId}/status
   */
  public void broadcastDriverStatusUpdate(Driver driver) {
    if (driver == null || driver.getId() == null) {
      log.warn("Cannot broadcast driver status: driver or driverId is null");
      return;
    }

    String driverStatusTopic = "/topic/driver/" + driver.getId() + "/status";
    try {
      messagingTemplate.convertAndSend(driverStatusTopic, Map.of(
          "driverId", driver.getId(),
          "isOnline", driver.getIsOnline() != null ? driver.getIsOnline() : false,
          "timestamp", java.time.LocalDateTime.now()
      ));
      log.debug("Broadcasted driver status update to: {}", driverStatusTopic);
    } catch (Exception e) {
      log.error("Error broadcasting driver status for driver {}: {}", driver.getId(), e.getMessage(), e);
    }
  }

  /**
   * Broadcast to all drivers (for new ride requests)
   * Topic: /topic/drivers/ride-requests
   */
  public void broadcastNewRideRequest(Ride ride, RideDto rideDto) {
    if (ride == null || ride.getId() == null) {
      log.warn("Cannot broadcast new ride request: ride or rideId is null");
      return;
    }

    try {
      String allDriversTopic = "/topic/drivers/ride-requests";
      messagingTemplate.convertAndSend(allDriversTopic, rideDto);
      log.debug("Broadcasted new ride request to all drivers: {}", allDriversTopic);
    } catch (Exception e) {
      log.error("Error broadcasting new ride request for ride {}: {}", ride.getId(), e.getMessage(), e);
    }
  }

  /**
   * Broadcast location update (already exists in TrackingController, but keeping for consistency)
   */
  public void broadcastLocationUpdate(Long rideId, Object locationUpdate) {
    if (rideId == null) {
      log.warn("Cannot broadcast location update: rideId is null");
      return;
    }

    try {
      String locationTopic = "/topic/ride/" + rideId + "/location";
      messagingTemplate.convertAndSend(locationTopic, locationUpdate);
      log.debug("Broadcasted location update to: {}", locationTopic);
    } catch (Exception e) {
      log.error("Error broadcasting location update for ride {}: {}", rideId, e.getMessage(), e);
    }
  }
}

