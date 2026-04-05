package org.sparta.secretary.message.presentation.dto;

import org.sparta.secretary.message.application.dto.SendResult;

public record MessageResponse(
        String receiverId,
        boolean isSuccess,
        String content,
        String sentTime
) {
    public static MessageResponse from(SendResult result) {
        return new MessageResponse(
                result.receiver(),
                result.success(),
                result.message(),
                result.sentAt().toString()
        );
    }
}