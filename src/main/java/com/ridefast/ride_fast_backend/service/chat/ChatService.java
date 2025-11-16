package com.ridefast.ride_fast_backend.service.chat;

import com.ridefast.ride_fast_backend.model.ChatMessage;

import java.util.List;

public interface ChatService {
    List<ChatMessage> listRideMessages(Long rideId);
    ChatMessage sendMessage(Long rideId, Long senderUserId, Long receiverUserId, String content);
}


