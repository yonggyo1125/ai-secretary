package org.sparta.secretary.global.tool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.sparta.secretary.global.tool.dto.Routes;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
public class NavigationToolsTest {

    @Autowired
    NavigationTools navigationTools;

    @Test
    void navigationToolsTest() {
        ToolContext context = new ToolContext(Map.of("apiKey", System.getenv("KAKAO_API_KEY")));
        List<Routes> routes = navigationTools.calculateRoutes(List.of("서울특별시 송파구 송파대로 55", "인천 남동구 정각로 29", "세종특별자치시 한누리대로 2130", "광주 서구 내방로 111", "울산 남구 중앙로 201", "충북 청주시 상당구 상당로 82"), context);
        log.info("Routes: {}", routes);
    }
}
