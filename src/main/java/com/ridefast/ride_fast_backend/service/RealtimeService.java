package com.ridefast.ride_fast_backend.service;

import com.ridefast.ride_fast_backend.dto.RideDto;
import com.ridefast.ride_fast_backend.enums.WalletOwnerType;
import com.ridefast.ride_fast_backend.model.Driver;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.websocket.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for broadcasting real-time updates via WebSocket
 * Compatible with Flutter WebSocket clients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final RealtimeWebSocketHandler webSocketHandler;

    // ==================== RIDE STATUS UPDATES ====================

    /**
     * Broadcast ride status update to user and driver
     * Event: "ride_status"
     * Rooms: "ride:{rideId}", "user:{userId}", "driver:{driverId}"
     */
    public void broadcastRideStatusUpdate(Ride ride, RideDto rideDto) {
        if (ride == null || ride.getId() == null) {
            log.warn("Cannot broadcast ride update: ride or rideId is null");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", ride.getId());
            payload.put("status", ride.getStatus().toString());
            payload.put("ride", rideDto);
            payload.put("timestamp", LocalDateTime.now());

            // Broadcast to ride-specific room
            String rideRoom = "ride:" + ride.getId();
            webSocketHandler.broadcastToRoom(rideRoom, "ride_status", payload);
            log.debug("Broadcasted ride status to room: {}", rideRoom);

            // Broadcast to user-specific room
            if (ride.getUser() != null && ride.getUser().getId() != null) {
                String userRoom = "user:" + ride.getUser().getId();
                webSocketHandler.broadcastToRoom(userRoom, "ride_status", payload);
                log.debug("Broadcasted ride status to user room: {}", userRoom);
            }

            // Broadcast to driver-specific room
            if (ride.getDriver() != null && ride.getDriver().getId() != null) {
                String driverRoom = "driver:" + ride.getDriver().getId();
                webSocketHandler.broadcastToRoom(driverRoom, "ride_status", payload);
                log.debug("Broadcasted ride status to driver room: {}", driverRoom);
            }
        } catch (Exception e) {
            log.error("Error broadcasting ride status for ride {}: {}", ride.getId(), e.getMessage(), e);
        }
    }

    /**
     * Broadcast new ride request to all available drivers
     * Event: "new_ride_request"
     * Room: "drivers:available"
     */
    public void broadcastNewRideRequest(Ride ride, RideDto rideDto) {
        if (ride == null || ride.getId() == null) {
            log.warn("Cannot broadcast new ride request: ride or rideId is null");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", ride.getId());
            payload.put("ride", rideDto);
            payload.put("timestamp", LocalDateTime.now());

            // Broadcast to all available drivers
            webSocketHandler.broadcastToRoom("drivers:available", "new_ride_request", payload);
            log.debug("Broadcasted new ride request to available drivers");
        } catch (Exception e) {
            log.error("Error broadcasting new ride request for ride {}: {}", ride.getId(), e.getMessage(), e);
        }
    }

    // ==================== DRIVER MOVEMENT UPDATES ====================

    /**
     * Broadcast driver location update
     * Event: "driver_location"
     * Rooms: "ride:{rideId}", "driver:{driverId}", "fleet:monitoring"
     */
    public void broadcastDriverLocation(Long rideId, Long driverId, Double lat, Double lng, Double heading) {
        if (rideId == null || driverId == null || lat == null || lng == null) {
            log.warn("Cannot broadcast driver location: missing required fields");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", rideId);
            payload.put("driverId", driverId);
            payload.put("lat", lat);
            payload.put("lng", lng);
            if (heading != null) payload.put("heading", heading);
            payload.put("timestamp", LocalDateTime.now());

            // Broadcast to ride room (for user tracking)
            String rideRoom = "ride:" + rideId;
            webSocketHandler.broadcastToRoom(rideRoom, "driver_location", payload);

            // Broadcast to driver room (for driver app)
            String driverRoom = "driver:" + driverId;
            webSocketHandler.broadcastToRoom(driverRoom, "driver_location", payload);

            // Broadcast to fleet monitoring room (for admin)
            webSocketHandler.broadcastToRoom("fleet:monitoring", "driver_location", payload);

            log.debug("Broadcasted driver location for ride {} driver {}", rideId, driverId);
        } catch (Exception e) {
            log.error("Error broadcasting driver location: {}", e.getMessage(), e);
        }
    }

    // ==================== DRIVER STATUS UPDATES ====================

    /**
     * Broadcast driver online/offline status
     * Event: "driver_status"
     * Rooms: "driver:{driverId}", "fleet:monitoring"
     */
    public void broadcastDriverStatusUpdate(Driver driver) {
        if (driver == null || driver.getId() == null) {
            log.warn("Cannot broadcast driver status: driver or driverId is null");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("driverId", driver.getId());
            payload.put("isOnline", driver.getIsOnline() != null ? driver.getIsOnline() : false);
            payload.put("latitude", driver.getLatitude());
            payload.put("longitude", driver.getLongitude());
            payload.put("timestamp", LocalDateTime.now());

            // Broadcast to driver-specific room
            String driverRoom = "driver:" + driver.getId();
            webSocketHandler.broadcastToRoom(driverRoom, "driver_status", payload);

            // Broadcast to fleet monitoring room (for admin)
            webSocketHandler.broadcastToRoom("fleet:monitoring", "driver_status", payload);

            log.debug("Broadcasted driver status for driver {}", driver.getId());
        } catch (Exception e) {
            log.error("Error broadcasting driver status for driver {}: {}", driver.getId(), e.getMessage(), e);
        }
    }

    // ==================== WALLET UPDATES ====================

    /**
     * Broadcast wallet transaction update
     * Event: "wallet_update"
     * Rooms: "user:{userId}", "driver:{driverId}"
     */
    public void broadcastWalletUpdate(String userId, WalletOwnerType ownerType, BigDecimal balance, 
                                     String transactionType, String description) {
        if (userId == null) {
            log.warn("Cannot broadcast wallet update: userId is null");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("ownerType", ownerType.toString());
            payload.put("balance", balance);
            payload.put("transactionType", transactionType);
            payload.put("description", description);
            payload.put("timestamp", LocalDateTime.now());

            String room = ownerType == WalletOwnerType.DRIVER ? "driver:" + userId : "user:" + userId;
            webSocketHandler.broadcastToRoom(room, "wallet_update", payload);
            log.debug("Broadcasted wallet update to room: {}", room);
        } catch (Exception e) {
            log.error("Error broadcasting wallet update: {}", e.getMessage(), e);
        }
    }

    // ==================== FLEET MONITORING ====================

    /**
     * Broadcast fleet statistics to admin
     * Event: "fleet_stats"
     * Room: "fleet:monitoring"
     */
    public void broadcastFleetStats(Map<String, Object> stats) {
        try {
            stats.put("timestamp", LocalDateTime.now());
            webSocketHandler.broadcastToRoom("fleet:monitoring", "fleet_stats", stats);
            log.debug("Broadcasted fleet stats to monitoring room");
        } catch (Exception e) {
            log.error("Error broadcasting fleet stats: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast driver list update to admin
     * Event: "fleet_drivers"
     * Room: "fleet:monitoring"
     */
    public void broadcastFleetDriversUpdate(Object drivers) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("drivers", drivers);
            payload.put("timestamp", LocalDateTime.now());
            webSocketHandler.broadcastToRoom("fleet:monitoring", "fleet_drivers", payload);
            log.debug("Broadcasted fleet drivers update");
        } catch (Exception e) {
            log.error("Error broadcasting fleet drivers: {}", e.getMessage(), e);
        }
    }

    // ==================== CHAT ====================

    /**
     * Broadcast chat message
     * Event: "chat_message"
     * Room: "ride:{rideId}"
     */
    public void broadcastChatMessage(Long rideId, String senderId, String senderName, 
                                    String receiverId, String message, String messageId) {
        if (rideId == null || message == null) {
            log.warn("Cannot broadcast chat message: missing required fields");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("rideId", rideId);
            payload.put("senderId", senderId);
            payload.put("senderName", senderName);
            payload.put("receiverId", receiverId);
            payload.put("message", message);
            payload.put("messageId", messageId);
            payload.put("timestamp", LocalDateTime.now());

            String rideRoom = "ride:" + rideId;
            webSocketHandler.broadcastToRoom(rideRoom, "chat_message", payload);
            log.debug("Broadcasted chat message to room: {}", rideRoom);
        } catch (Exception e) {
            log.error("Error broadcasting chat message: {}", e.getMessage(), e);
        }
    }
}

