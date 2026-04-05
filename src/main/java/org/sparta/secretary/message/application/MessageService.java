package org.sparta.secretary.message.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatClient chatClient;

    public Mono<SendResult> sendMessage(String slackId, String formatDirection, Map<String, Object> params) {
        log.info("슬랙 메시지 전송 및 결과 기록 시작 - 대상: {}", slackId);

        return Mono.fromCallable(() -> {
            try {
                // AI 호출 및 결과를 SendResult 객체로 매핑 (동기 블로킹 구간)
                SendResult result = this.chatClient.prompt()
                        .user(u -> u.text("""
                                당신은 슬랙 보고 전문가입니다. 다음 단계를 거쳐 업무를 수행하고 결과를 보고하세요.
                                
                                1. 변환: 'toConvertMessage' 도구를 사용하여 데이터를 지침({format})에 맞게 슬랙 마크다운으로 변환하세요.
                                   - 데이터: {data}
                                2. 전송: 변환된 메시지를 'sendMessage' 도구를 사용하여 슬랙 ID({slackId})로 전송하세요.
                                3. 기록: 모든 과정이 끝나면 최종 결과를 'SendResult' 형식에 맞춰 응답하세요.
                                
                                [필드 구성 지침]
                                - receiver: 전달받은 슬랙 ID ({slackId})
                                - success: 실제 'sendMessage' 도구 호출 성공 여부 (true/false)
                                - message: 변환되어 실제로 전송된 최종 메시지 내용
                                - reason: 이 메시지를 전송하게 된 배경이나 변환 시 중점을 둔 이유 요약
                                - sentAt: 현재 시간 (ISO 8601 형식)
                                """)
                                .param("slackId", slackId)
                                .param("format", formatDirection)
                                .param("data", params == null ? "없음" : params.toString()))
                        .call()
                        .entity(SendResult.class);

                if (result == null) {
                    throw new RuntimeException("AI가 응답 객체를 생성하지 못했습니다.");
                }

                // 결과 보정 및 반환
                return new SendResult(
                        result.receiver() != null ? result.receiver() : slackId,
                        result.success(),
                        result.message() != null ? result.message() : "내용 없음",
                        result.reason() != null ? result.reason() : "자동 보고",
                        result.sentAt() == null ? LocalDateTime.now() : result.sentAt()
                );

            } catch (Exception e) {
                // 에러 로그 기록
                log.error("슬랙 메시지 발송 프로세스 중 오류 발생: {}", e.getMessage(), e);
                // 비즈니스 예외로 래핑하여 던짐 (Mono.error로 전환됨)
                throw new RuntimeException("메시지 발송 실패: " + e.getMessage());
            }
        });
    }
}
