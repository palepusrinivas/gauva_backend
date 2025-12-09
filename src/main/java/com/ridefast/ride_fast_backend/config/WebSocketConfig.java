package com.ridefast.ride_fast_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.ridefast.ride_fast_backend.websocket.RealtimeWebSocketHandler;

/**
 * WebSocket configuration for Flutter-compatible real-time communication
 * Flutter clients can connect to: ws://host:port/ws
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler webSocketHandler;

    public WebSocketConfig(RealtimeWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket endpoint - Flutter can connect to ws://host:port/ws
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("*"); // Configure CORS as needed for production
    }
}

