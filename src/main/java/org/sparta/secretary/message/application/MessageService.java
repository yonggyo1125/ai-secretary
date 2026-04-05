package org.sparta.secretary.message.application;

import lombok.extern.slf4j.Slf4j;
import org.sparta.secretary.global.tool.NavigationTools;
import org.sparta.secretary.global.tool.SlackTools;
import org.sparta.secretary.message.application.dto.SendResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class MessageService {

    private final ChatClient chatClient;

    public MessageService(ChatClient.Builder builder,
                                 @Value("${KAKAO_API_KEY}") String apiKey,
                                 @Value("${SLACK_TOKEN}") String token,
                           NavigationTools navigationTools,
                           SlackTools slackTools) {
       this.chatClient = builder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .defaultTools(navigationTools, slackTools)
                .defaultToolContext(Map.of("apiKey", apiKey, "token", token))
                .build();
    }

    public Mono<SendResult> sendMessage(String slackId, String formatDirection, Map<String, Object> params) {
        log.info("슬랙 메시지 전송 및 결과 기록 시작 - 대상: {}", slackId);

        return Mono.fromCallable(() -> {
                    try {
                        // 1. AI 호출 및 도구 실행 (동기 블로킹 구간)
                        SendResult result = this.chatClient.prompt()
                                .user(u -> u.text("""
                            당신은 제공된 도구를 활용하여 데이터를 정제하고 슬랙으로 전송하는 전문가입니다. 
                            이전의 대화 맥락은 무시하고 오직 다음의 단계별 명령을 수행하십시오.
                            
                            [단계 1: 데이터 분석 및 경로 산출 (조건부)]
                            - 제공된 [데이터] 내에 **실제 주소나 장소 정보가 2개 이상** 포함되어 있는지 확인하십시오.
                            - 주소가 2개 이상 있다면, 'calculateRoutes' 도구를 사용하여 최적 경로와 시간을 산출하십시오.
                            - **주의**: 주소가 없거나 1개뿐이라면 절대 'calculateRoutes'를 호출하지 마십시오.
                            
                            [단계 2: 메시지 변환]
                            'toConvertMessage' 도구를 호출하여 아래 데이터를 슬랙 마크다운 형식으로 변환하십시오.
                            **중요**: 호출 시 아래 [데이터] 섹션의 값을 'params' 인자로 반드시 전달해야 합니다.
                            - 형식 지침: {format}
                            - 대상 데이터: {data}

                            [단계 3: 이동 경로 확인 (선택 사항)]
                            만약 데이터에 장소 정보나 주소가 포함되어 있다면, 'calculateRoutes' 도구를 사용하여 최적 경로를 산출한 뒤 메시지에 포함시키십시오.

                            [단계 4: 슬랙 전송]
                            변환된 최종 메시지를 'sendMessage' 도구를 사용하여 전송하십시오.
                            - 대상 슬랙 ID: {slackId}

                            [단계 5: 결과 기록]
                            모든 도구 실행이 완료되면, 최종 실행 결과를 'SendResult' 형식의 JSON으로만 응답하십시오.
                            - sentAt 필드 주의: 'yyyy-MM-ddTHH:mm:ss' 형식으로 작성하고, 끝에 'Z'를 절대 붙이지 마십시오. (예: 2026-04-05T21:22:34)
                            
                            [필드 가이드]
                            - receiver: {slackId}
                            - success: sendMessage 도구의 최종 성공 여부 (true/false)
                            - message: 실제로 슬랙에 전송된 전체 텍스트
                            - reason: "데이터 {data}를 기반으로 {format} 지침에 따라 변환 후 전송 완료"
                            - sentAt: 현재 시간 (ISO 8601)
                            """)
                                        .param("slackId", slackId)
                                        .param("format", formatDirection)
                                        .param("data", params == null ? Map.of(): params))
                                .call()
                                .entity(SendResult.class);

                        if (result == null) {
                            throw new RuntimeException("AI가 응답 객체를 생성하지 못했습니다.");
                        }

                        // 2. 결과 보정 및 반환
                        return new SendResult(
                                result.receiver() != null ? result.receiver() : slackId,
                                result.success(),
                                result.message() != null ? result.message() : "내용 없음",
                                result.reason() != null ? result.reason() : "자동 보고",
                                !StringUtils.hasText(result.sentAt()) ? LocalDateTime.now().toString() : result.sentAt()
                        );

                    } catch (Exception e) {
                        log.error("슬랙 메시지 발송 프로세스 중 오류 발생: {}", e.getMessage(), e);
                        throw new RuntimeException("메시지 발송 실패: " + e.getMessage());
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
