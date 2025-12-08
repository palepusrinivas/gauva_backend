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
        try {
            socketIOServer.start();
            log.info("Socket.IO server started successfully");
        } catch (Exception e) {
            log.error("Failed to start Socket.IO server", e);
            throw new RuntimeException("Socket.IO server startup failed", e);
        }
    }
}

