package org.sparta.secretary.global.tool;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@Slf4j
@SpringBootTest
public class SlackToolsTest {

    @Autowired
    SlackTools slackTools;

    @Test
    void slackMessageTest() {
        ToolContext context = new ToolContext(Map.of("token", System.getenv("SLACK_TOKEN")));
        String response = slackTools.sendMessage("U09EGL79XGX", "테스트 메세지", context);
        log.info("response: {}", response);
    }
}
