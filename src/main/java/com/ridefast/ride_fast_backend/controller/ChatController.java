package com.ridefast.ride_fast_backend.controller;

import com.ridefast.ride_fast_backend.dto.ChatMessageRequest;
import com.ridefast.ride_fast_backend.model.ChatMessage;
import com.ridefast.ride_fast_backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/ride/{rideId}/messages")
    public ResponseEntity<List<ChatMessage>> listRideMessages(@PathVariable Long rideId) {
        return ResponseEntity.ok(chatService.listRideMessages(rideId));
    }

    @PostMapping("/ride/{rideId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long rideId,
                                         @RequestBody @Validated ChatMessageRequest req) {
        if (req.getContent().isBlank()) {
            return new ResponseEntity<>(Map.of("error", "content must not be blank"), HttpStatus.BAD_REQUEST);
        }
        ChatMessage saved = chatService.sendMessage(rideId, req.getSenderUserId(), req.getReceiverUserId(), req.getContent());
        messagingTemplate.convertAndSend("/topic/chat/ride/" + rideId, saved);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
}


