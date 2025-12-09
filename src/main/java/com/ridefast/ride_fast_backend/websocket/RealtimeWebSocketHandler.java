package com.ridefast.ride_fast_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for real-time communication with Flutter clients
 * Handles connections, disconnections, and message routing
 */
@Component
@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store active sessions by session ID
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Store sessions by room (e.g., "ride:123", "user:userId", "driver:driverId")
    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    
    // Store user/driver IDs for each session
    private final Map<String, String> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDriverIds = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket client connected: {}", session.getId());
        
        // Send welcome message
        sendMessage(session, "connected", Map.of(
            "sessionId", session.getId(),
            "message", "Connected to WebSocket server"
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        // Remove from all rooms
        rooms.values().forEach(roomSessions -> roomSessions.remove(session));
        
        // Clean up user/driver mappings
        sessionUserIds.remove(sessionId);
        sessionDriverIds.remove(sessionId);
        
        log.info("WebSocket client disconnected: {} - Status: {}", sessionId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received message from {}: {}", session.getId(), payload);
            
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String event = (String) data.get("event");
            
            if (event == null) {
                sendError(session, "Missing 'event' field in message");
                return;
            }
            
            switch (event) {
                case "join":
                    handleJoin(session, data);
                    break;
                case "leave":
                    handleLeave(session, data);
                    break;
                case "location":
                    handleLocationUpdate(session, data);
                    break;
                case "chat":
                    handleChatMessage(session, data);
                    break;
                case "ping":
                    sendMessage(session, "pong", Map.of("timestamp", System.currentTimeMillis()));
                    break;
                default:
                    log.warn("Unknown event type: {}", event);
                    sendError(session, "Unknown event type: " + event);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle join room request
     * Expected format: {"event": "join", "type": "ride|user|driver|fleet", "id": "123"}
     */
    private void handleJoin(WebSocketSession session, Map<String, Object> data) {
        String type = (String) data.get("type");
        Object idObj = data.get("id");
        
        if (type == null || idObj == null) {
            sendError(session, "Missing 'type' or 'id' in join request");
            return;
        }
        
        String room = buildRoomName(type, idObj.toString());
        rooms.computeIfAbsent(room, k -> new CopyOnWriteArraySet<>()).add(session);
        
        // Store user/driver ID for this session
        if ("user".equals(type)) {
            sessionUserIds.put(session.getId(), idObj.toString());
        } else if ("driver".equals(type)) {
            sessionDriverIds.put(session.getId(), idObj.toString());
        }
        
        log.info("Session {} joined room: {}", session.getId(), room);
        sendMessage(session, "joined", Map.of("room", room, "type", type));
    }

    /**
     * Handle leave room request
     */
    private void handleLeave(WebSocketSession session, Map<String, Object> data) {
        String type = (String) data.get("type");
        Object idObj = data.get("id");
        
        if (type != null && idObj != null) {
            String room = buildRoomName(type, idObj.toString());
            CopyOnWriteArraySet<WebSocketSession> roomSessions = rooms.get(room);
            if (roomSessions != null) {
                roomSessions.remove(session);
                log.info("Session {} left room: {}", session.getId(), room);
            }
        }
    }

    /**
     * Handle location update from driver
     * Expected format: {"event": "location", "rideId": 123, "driverId": 456, "lat": 17.5, "lng": 78.3, "heading": 90}
     */
    private void handleLocationUpdate(WebSocketSession session, Map<String, Object> data) {
        Object rideIdObj = data.get("rideId");
        Object driverIdObj = data.get("driverId");
        
        if (rideIdObj == null) {
            sendError(session, "Missing 'rideId' in location update");
            return;
        }
        
        // Broadcast to ride room
        String rideRoom = "ride:" + rideIdObj.toString();
        broadcastToRoom(rideRoom, "driver_location", data);
        
        // Broadcast to driver room if driverId provided
        if (driverIdObj != null) {
            String driverRoom = "driver:" + driverIdObj.toString();
            broadcastToRoom(driverRoom, "driver_location", data);
        }
        
        // Broadcast to fleet monitoring
        broadcastToRoom("fleet:monitoring", "driver_location", data);
    }

    /**
     * Handle chat message
     * Expected format: {"event": "chat", "rideId": 123, "senderId": "user123", "message": "Hello"}
     */
    private void handleChatMessage(WebSocketSession session, Map<String, Object> data) {
        Object rideIdObj = data.get("rideId");
        
        if (rideIdObj == null) {
            sendError(session, "Missing 'rideId' in chat message");
            return;
        }
        
        String rideRoom = "ride:" + rideIdObj.toString();
        data.put("timestamp", System.currentTimeMillis());
        broadcastToRoom(rideRoom, "chat_message", data);
    }

    /**
     * Broadcast message to a specific room
     */
    public void broadcastToRoom(String room, String event, Object data) {
        CopyOnWriteArraySet<WebSocketSession> roomSessions = rooms.get(room);
        if (roomSessions == null || roomSessions.isEmpty()) {
            return;
        }
        
        Map<String, Object> message = Map.of(
            "event", event,
            "data", data,
            "timestamp", System.currentTimeMillis()
        );
        
        roomSessions.forEach(session -> {
            try {
                sendMessage(session, event, data);
            } catch (Exception e) {
                log.error("Error broadcasting to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    /**
     * Send message to a specific session
     */
    public void sendToSession(String sessionId, String event, Object data) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, event, data);
        }
    }

    /**
     * Send message to all sessions in a room
     */
    private void sendMessage(WebSocketSession session, String event, Object data) {
        try {
            Map<String, Object> message = Map.of(
                "event", event,
                "data", data,
                "timestamp", System.currentTimeMillis()
            );
            
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Send error message to session
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                "event", "error",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
            );
            String json = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Error sending error message: {}", e.getMessage());
        }
    }

    /**
     * Build room name from type and id
     */
    private String buildRoomName(String type, String id) {
        switch (type) {
            case "ride":
                return "ride:" + id;
            case "user":
                return "user:" + id;
            case "driver":
                return "driver:" + id;
            case "fleet":
                return "fleet:monitoring";
            case "drivers":
                return "drivers:available";
            default:
                return type + ":" + id;
        }
    }

    /**
     * Get all active sessions (for monitoring)
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Get sessions in a room
     */
    public int getRoomSessionCount(String room) {
        CopyOnWriteArraySet<WebSocketSession> roomSessions = rooms.get(room);
        return roomSessions != null ? roomSessions.size() : 0;
    }
}

