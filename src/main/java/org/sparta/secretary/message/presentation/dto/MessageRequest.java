package org.sparta.secretary.message.presentation.dto;

import java.util.Map;

public record MessageRequest(
        String format,       // 메시지 구성 지침
        Map<String, Object> params // 메시지에 포함될 동적 데이터
) {
    public String getFormatOrDefault() {
        return (format != null && !format.isBlank()) ? format : "정중한 요약 보고 형식으로 작성해줘";
    }
}
