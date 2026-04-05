package org.sparta.secretary.global.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class SlackTools {

    private final WebClient webClient;
    private final ChatClient chatClient;

    public SlackTools(WebClient.Builder webClientBuilder, ChatModel chatModel) {
        this.webClient = webClientBuilder
                .baseUrl("https://slack.com/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Tool(description = "제공된 파라미터(JSON 데이터 등)를 기반으로, 지정된 형식 지침에 따라 슬랙 전송용 메시지 본문을 생성합니다. " +
            "단순 치환이 아닌 자연스러운 문장으로 구성하며, 슬랙 마크다운(Markdown) 형식을 지원합니다.")
    public String toConvertMessage(
            @ToolParam(description = "메시지의 톤앤매너나 필수 포함 정보를 정의하는 지침 (예: '일정 요약 형식으로 정중하게 작성해줘')") String formatDirection,
            @ToolParam(description = "메시지에 녹여낼 실제 데이터들 (경로 정보, 시간, 장소명 등)") Map<String, Object> params) {

        try {
            log.info("LLM 메시지 변환 시작 - 지침: {}", formatDirection);

            return this.chatClient.prompt()
                    .system("""
                            당신은 슬랙(Slack) 메시지 작성 전문가입니다. 
                            제공된 데이터를 바탕으로 사용자의 지침에 따라 가독성 좋은 메시지를 작성하세요.
                            - 슬랙의 마크다운(bold: *text*, list: - 등)을 적절히 사용하세요.
                            - 불필요한 서술은 생략하고 핵심 정보를 명확히 전달하세요.
                            - 특수문자(&, <, >) 처리에 유의하세요.
                            """)
                    .user(u -> u.text("""
                            [형식지침]
                            {format}
                            
                            [데이터 데이터]
                            {data}
                            """)
                            .param("format", formatDirection)
                            .param("data", params == null ? "" : params.toString()))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("메시지 변환 중 AI 호출 실패: {}", e.getMessage());
            return "메시지 생성 중 오류가 발생했습니다. 원본 데이터를 전달합니다: " + params.toString();
        }
    }

    @Tool(description = "지정된 슬랙 사용자에게 메세지를 전송합니다.")
    public String sendMessage(@ToolParam(description="메세지를 받을 슬랙 사용자 ID 또는 채널 ID") String slackId, @ToolParam(description = "전송할 메세지 내용") String message, ToolContext context) {
        Object tokenObj = context.getContext().get("token");
        if (tokenObj == null) {
            log.warn("Slack 전송 실패: Context에 token이 존재하지 않음");
            return "Slack Token이 존재하지 않습니다. 설정을 확인해주세요.";
        }
        String slackToken = tokenObj.toString();

        try {
            log.debug("Slack 메시지 전송 시도: 대상={}, 내용={}", slackId, message);
            ResponseEntity<JsonNode> res = webClient.post()
                    .uri("/chat.postMessage")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + slackToken)
                    .bodyValue(Map.of(
                                "channel", slackId,
                                "text", message))
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block(java.time.Duration.ofSeconds(10));

            if (res != null && res.getStatusCode().is2xxSuccessful()) {
                JsonNode body = res.getBody();

                // 슬랙은 HTTP 200을 응답해도 내부 로직 실패 시 'ok: false'를 반환
                if (body != null && body.get("ok").asBoolean()) {
                    String ts = body.get("ts").asText();
                    log.info("슬랙 전송 성공: [ID: {}, TS: {}]", slackId, ts);
                    return "전송 성공 (Timestamp: " + ts + ")";
                } else {
                    String error = (body != null && body.has("error")) ? body.get("error").asText() : "unknown_error";
                    log.error("슬랙 API 비즈니스 에러: {}", error);
                    return "슬랙 API 오류: " + error;
                }
            } else {
                return "HTTP 통신 실패 (Status: " + (res != null ? res.getStatusCode() : "No Response") + ")";
            }
        } catch (Exception e) {
            log.error("슬랙 전송 중 시스템 예외 발생: {}", e.getMessage());
            return "시스템 오류로 전송에 실패했습니다: " + e.getMessage();
        }
    }
}
