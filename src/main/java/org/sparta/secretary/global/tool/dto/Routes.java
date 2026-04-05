package org.sparta.secretary.global.tool.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record Routes(
    String address, // 주소
    Coordinates coordinates, // 해당 지점의 위경도 좌표

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime estimatedTime, // 도착 예상 시간
    Integer durationMinutes, // 이전 지점으로부터 소요 시간 (분 단위, 선택 사항)
    String reason // 해당 경로 선정 이유 (예: 최단 거리, 교통 정체 회피 등)
) {}
