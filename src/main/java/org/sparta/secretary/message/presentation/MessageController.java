package org.sparta.secretary.message.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sparta.secretary.message.application.MessageService;
import org.sparta.secretary.message.presentation.dto.MessageRequest;
import org.sparta.secretary.message.presentation.dto.MessageResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/message")
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/{slackId}")
    public Mono<MessageResponse> sendSlackMessage(
            @PathVariable String slackId,
            @RequestBody MessageRequest request) {

        log.info("회원 슬랙 메시지 요청 접수 - 대상: {}", slackId);

        return messageService.sendMessage(
                        slackId,
                        request.getFormatOrDefault(),
                        request.params()
                )
                .map(MessageResponse::from)
                .doOnSuccess(res -> log.info("메시지 전송 프로세스 완료: {}", res.receiverId()))
                .doOnError(e -> log.error("메시지 전송 중 오류 발생: {}", e.getMessage()));
    }
}
