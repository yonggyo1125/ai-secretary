package org.sparta.secretary.chatbot.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.secretary.chatbot.application.ChatBotService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @GetMapping
    public String index() {
        return "chatbot/index";
    }

    @ResponseBody
    @PostMapping(consumes = "text/plain;charset=UTF-8", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> process(@RequestBody String question) {
        return chatBotService.generate(question);
    }
}
