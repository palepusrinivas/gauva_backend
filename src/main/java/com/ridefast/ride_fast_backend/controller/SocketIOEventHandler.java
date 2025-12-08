package com.ridefast.ride_fast_backend.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Socket.IO event handlers
 * Handles connection, disconnection, and custom events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIOEventHandler {

    private final SocketIOServer socketIOServer;

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Socket.IO client connected: {}", client.getSessionId());
        // You can add authentication here if needed
        // For example, check JWT token from handshake data
        Map<String, java.util.List<String>> handshakeData = client.getHandshakeData().getUrlParams();
        log.debug("Handshake data: {}", handshakeData);
        
        // Send welcome message
        client.sendEvent("connected", Map.of(
                "sessionId", client.getSessionId().toString(),
                "message", "Connected to Socket.IO server"
        ));
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        log.info("Socket.IO client disconnected: {}", client.getSessionId());
    }

    /**
     * Handle driver location updates
     * Client can send: socket.emit('location', { lat: 17.5, lng: 78.3, rideId: 1, driverId: 6, heading: 90 })
     */
    @OnEvent("location")
    public void onLocationUpdate(SocketIOClient client, Map<String, Object> data) {
        log.debug("Location update from {}: {}", client.getSessionId(), data);
        try {
            Long rideId = data.get("rideId") != null ? ((Number) data.get("rideId")).longValue() : null;
            Long driverId = data.get("driverId") != null ? ((Number) data.get("driverId")).longValue() : null;
            Double lat = data.get("lat") != null ? ((Number) data.get("lat")).doubleValue() : null;
            Double lng = data.get("lng") != null ? ((Number) data.get("lng")).doubleValue() : null;
            Double heading = data.get("heading") != null ? ((Number) data.get("heading")).doubleValue() : null;
            
            if (rideId != null && lat != null && lng != null) {
                // Broadcast to ride room, driver room, and fleet monitoring
                String rideRoom = "ride:" + rideId;
                socketIOServer.getRoomOperations(rideRoom).sendEvent("driver_location", data);
                
                if (driverId != null) {
                    String driverRoom = "driver:" + driverId;
                    socketIOServer.getRoomOperations(driverRoom).sendEvent("driver_location", data);
                }
                
                // Also broadcast to fleet monitoring (for admin)
                socketIOServer.getRoomOperations("fleet:monitoring").sendEvent("driver_location", data);
            }
        } catch (Exception e) {
            log.error("Error handling location update: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle chat messages between driver and rider
     * Client can send: socket.emit('chat', { 
     *   rideId: 1, 
     *   senderId: "user123", 
     *   senderName: "John Doe",
     *   receiverId: "driver456",
     *   message: "Hello",
     *   messageId: "msg-123"
     * })
     */
    @OnEvent("chat")
    public void onChatMessage(SocketIOClient client, Map<String, Object> data) {
        log.debug("Chat message from {}: {}", client.getSessionId(), data);
        try {
            Long rideId = data.get("rideId") != null ? ((Number) data.get("rideId")).longValue() : null;
            if (rideId != null) {
                String rideRoom = "ride:" + rideId;
                // Broadcast to all participants in the ride room
                socketIOServer.getRoomOperations(rideRoom).sendEvent("chat_message", data);
                log.info("Chat message broadcasted to room: {}", rideRoom);
            }
        } catch (Exception e) {
            log.error("Error handling chat message: {}", e.getMessage(), e);
        }
    }

    /**
     * Join a room (e.g., for a specific ride, user, driver, or fleet monitoring)
     * Client can send: socket.emit('join', { 
     *   type: "ride",    // "ride", "user", "driver", "fleet", "drivers"
     *   id: 1            // rideId, userId, driverId, or "monitoring"/"available"
     * })
     */
    @OnEvent("join")
    public void onJoinRoom(SocketIOClient client, Map<String, Object> data) {
        try {
            String type = (String) data.get("type");
            Object idObj = data.get("id");
            
            String room = null;
            if ("ride".equals(type) && idObj != null) {
                Long rideId = ((Number) idObj).longValue();
                room = "ride:" + rideId;
            } else if ("user".equals(type) && idObj != null) {
                room = "user:" + idObj.toString();
            } else if ("driver".equals(type) && idObj != null) {
                Long driverId = ((Number) idObj).longValue();
                room = "driver:" + driverId;
            } else if ("fleet".equals(type)) {
                room = "fleet:monitoring";
            } else if ("drivers".equals(type)) {
                room = "drivers:available";
            }
            
            if (room != null) {
                client.joinRoom(room);
                log.info("Client {} joined room {}", client.getSessionId(), room);
                client.sendEvent("joined", Map.of("room", room, "type", type));
            } else {
                client.sendEvent("join_error", Map.of("message", "Invalid join request"));
            }
        } catch (Exception e) {
            log.error("Error joining room: {}", e.getMessage(), e);
            client.sendEvent("join_error", Map.of("message", e.getMessage()));
        }
    }

    /**
     * Example: Leave a room
     * Client can send: socket.emit('leave', { rideId: 1 })
     */
    @OnEvent("leave")
    public void onLeaveRoom(SocketIOClient client, Map<String, Object> data) {
        Long rideId = ((Number) data.get("rideId")).longValue();
        String room = "ride:" + rideId;
        client.leaveRoom(room);
        log.info("Client {} left room {}", client.getSessionId(), room);
    }
}

