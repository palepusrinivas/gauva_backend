package com.ridefast.ride_fast_backend.config;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Starts the Socket.IO server when Spring Boot application starts
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SocketIOServerRunner implements CommandLineRunner {

    private final SocketIOServer socketIOServer;

    @Override
    public void run(String... args) {
        if (socketIOServer == null) {
            log.info("Socket.IO server is disabled, skipping startup");
            return;
        }
        
        try {
            socketIOServer.start();
            log.info("Socket.IO server started successfully on port {}", 
                socketIOServer.getConfiguration().getPort());
        } catch (Exception e) {
            log.error("Failed to start Socket.IO server. This is expected on Azure App Service " +
                    "where only one port is exposed. Socket.IO will not be available.", e);
            // Don't throw exception - allow app to continue without Socket.IO
            // Azure App Service only exposes the main application port, so Socket.IO can't run
            log.warn("Application will continue without Socket.IO support. Use STOMP WebSocket at /ws instead.");
        }
    }
}

