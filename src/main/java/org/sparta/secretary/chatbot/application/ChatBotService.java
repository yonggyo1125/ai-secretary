package org.sparta.secretary.chatbot.application;

import reactor.core.publisher.Flux;

public interface ChatBotService {
    Flux<String> generate(String question);
}
