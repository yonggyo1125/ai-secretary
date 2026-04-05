package org.sparta.secretary.chatbot.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.secretary.chatbot.application.ChatBotService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final ChatClient chatClient;

    @Override
    public Flux<String> generate(String question) {
        log.info("사용자 질문 접수: {}", question);

        return this.chatClient.prompt()
                .user(question)
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("채팅 생성 중 시스템 예외 발생: {}", e.getMessage(), e);
                    String errorMessage = "요청하신 작업을 처리하는 중에 문제가 발생했습니다.\n" +
                            "(사유: " + e.getMessage() + ")\n" +
                            "잠시 후 다시 시도해 주세요.";
                    return Flux.just(errorMessage);
                })
                .doOnComplete(() -> log.info("답변 생성 완료"));
    }
}
