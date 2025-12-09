package com.ridefast.ride_fast_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller to handle legacy Socket.IO requests
 * Returns helpful error message directing clients to use the new WebSocket endpoint
 */
@RestController
@RequestMapping("/socket.io")
public class SocketIORedirectController {

    @GetMapping("/**")
    public ResponseEntity<Map<String, Object>> handleSocketIORequest() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "Socket.IO is no longer supported",
                "message", "This application now uses standard WebSocket. Please connect to ws://host:port/ws instead.",
                "newEndpoint", "/ws",
                "documentation", "See FLUTTER_WEBSOCKET_INTEGRATION.md for migration guide"
            ));
    }
}

