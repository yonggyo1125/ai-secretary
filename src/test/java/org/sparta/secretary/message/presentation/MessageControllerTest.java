package org.sparta.secretary.message.presentation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sparta.secretary.message.presentation.dto.MessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
@AutoConfigureWebTestClient
public class MessageControllerTest {
    @Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(50))
                .build();
    }

    @Test
    void messageApiTest() throws Exception {
        String slackId = "U09EGL79XGX";
        List<String> addresses = List.of(
                "서울특별시 송파구 송파대로 55",
                "인천 남동구 정각로 29",
                "세종특별자치시 한누리대로 2130",
                "광주 서구 내방로 111",
                "울산 남구 중앙로 201",
                "충북 청주시 상당구 상당로 82"
        );
        String format = """
                물류 이동을 담당하는 택배기사에게 그날 배송할 경유지들을 이동할 때의 최적의 경로를 안내하는 메세지를 완성해주세요.
                [경유지목록]
                %s
                
                [형식지침]
                - 이동순번: 1
                - 이동구간: 서울특별시 송파구 송파대로 55 -> 인천 남동구 정각로 29
                - 소요시간: 1.3시간
                - 소요거리: 30.5km
                -----------------------------------------------------
                - 이동순번: 2
                - 이동구간: 서울특별시 송파구 송파대로 55 -> 인천 남동구 정각로 29
                - 소요시간: 1.3시간
                - 소요거리: 30.5km
                -----------------------------------------------------
                - 이동순번: 3
                - 이동구간: 서울특별시 송파구 송파대로 55 -> 인천 남동구 정각로 29
                - 소요시간: 1.3시간
                - 소요거리: 30.5km
                """.formatted(String.join("\n", addresses));



        MessageRequest request = new MessageRequest(format, Map.of("address", addresses));

        webTestClient.post()
                .uri("/api/v1/message/{slackId}", slackId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), MessageRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .consumeWith(result -> {
                    log.info("응답 결과: {}", new String(result.getResponseBodyContent()));
                });

    }
}
