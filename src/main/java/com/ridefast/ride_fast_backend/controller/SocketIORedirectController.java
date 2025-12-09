package com.ridefast.ride_fast_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to handle legacy Socket.IO requests
 * Returns helpful error message directing clients to use the new WebSocket endpoint
 */
@RestController
@RequestMapping("/socket.io")
public class SocketIORedirectController {

    /**
     * Handle all Socket.IO requests (GET, POST, etc.)
     * Socket.IO clients may try various HTTP methods for handshake
     */
    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> handleSocketIORequest() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "Socket.IO is no longer supported",
                "message", "This application now uses standard WebSocket. Please connect to ws://host:port/ws instead.",
                "newEndpoint", "/ws",
                "migrationGuide", "See FLUTTER_WEBSOCKET_INTEGRATION.md for migration guide",
                "note", "Socket.IO has been replaced with standard WebSocket for better Flutter compatibility"
            ));
    }
}

