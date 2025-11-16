package com.ridefast.ride_fast_backend.service.chat.impl;

import com.ridefast.ride_fast_backend.model.ChatMessage;
import com.ridefast.ride_fast_backend.model.Ride;
import com.ridefast.ride_fast_backend.repository.ChatMessageRepository;
import com.ridefast.ride_fast_backend.repository.RideRepository;
import com.ridefast.ride_fast_backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatRepo;
    private final RideRepository rideRepo;

    @Override
    public List<ChatMessage> listRideMessages(Long rideId) {
        Ride ride = rideRepo.findById(rideId).orElseThrow();
        return chatRepo.findByRideOrderByCreatedAtAsc(ride);
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(Long rideId, Long senderUserId, Long receiverUserId, String content) {
        Ride ride = rideRepo.findById(rideId).orElseThrow();
        ChatMessage m = ChatMessage.builder()
                .ride(ride)
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        return chatRepo.save(m);
    }
}


