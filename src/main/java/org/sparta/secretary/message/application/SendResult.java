package org.sparta.secretary.message.application;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record SendResult(
        String receiver, // 메세지 받는분 슬랙 ID
        boolean success, // 메세지 전송 성공 여부
        String message, // 전송된 메세지
        String reason, // 변환한 이유
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime sentAt // 메세지 발송 시간
) {
}
