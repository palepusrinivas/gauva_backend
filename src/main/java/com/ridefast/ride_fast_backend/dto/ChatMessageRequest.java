package com.ridefast.ride_fast_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotNull
    private Long senderUserId;
    @NotNull
    private Long receiverUserId;
    @NotBlank
    private String content;
}


