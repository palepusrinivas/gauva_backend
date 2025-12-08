package com.ridefast.ride_fast_backend.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Socket.IO server configuration
 * This provides Socket.IO protocol support alongside STOMP WebSocket
 * 
 * Note: Socket.IO server runs on the same port as Spring Boot by default
 * The client should connect to: http://localhost:8080/socket.io/
 */
@Configuration
@Slf4j
public class SocketIOConfig {

    // Socket.IO runs on a separate port since it uses Netty server
    // Default to server.port + 1, or set socketio.port explicitly
    @Value("${socketio.port:9090}")
    private int socketIOPort;

    @Value("${socketio.host:0.0.0.0}")
    private String socketIOHost;

    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketIOHost);
        config.setPort(socketIOPort);
        config.setAllowCustomRequests(true);
        config.setUpgradeTimeout(10000);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);
        config.setMaxHttpContentLength(1024 * 1024); // 1MB
        config.setBossThreads(1);
        config.setWorkerThreads(100);
        
        // Allow all origins (adjust for production)
        config.setOrigin("*");
        
        // Enable both WebSocket and Polling transports
        // Note: netty-socketio uses Socket.IO v1.x protocol (EIO=3), not v4 (EIO=4)
        config.setTransports(com.corundumstudio.socketio.Transport.WEBSOCKET, 
                           com.corundumstudio.socketio.Transport.POLLING);
        
        server = new SocketIOServer(config);
        log.info("Socket.IO server configured on {}:{}", socketIOHost, socketIOPort);
        log.warn("IMPORTANT: netty-socketio library uses Socket.IO v1.x protocol (EIO=3), not v4 (EIO=4). " +
                "Clients must connect with EIO=3 in the URL: http://{}:{}/socket.io/?EIO=3&transport=websocket", 
                socketIOHost, socketIOPort);
        return server;
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketIOServer) {
        return new SpringAnnotationScanner(socketIOServer);
    }

    @PreDestroy
    public void stopSocketIOServer() {
        if (server != null) {
            log.info("Stopping Socket.IO server...");
            server.stop();
        }
    }
}

