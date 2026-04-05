package org.sparta.secretary.global.config;

import lombok.RequiredArgsConstructor;
import org.sparta.secretary.global.tool.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AiConfig {

    private final DateTimeTools dateTimeTools;
    private final FileSystemTools fileSystemTools;
    private final InternetSearchTools internetSearchTools;
    private final NavigationTools navigationTools;
    private final SlackTools slackTools;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/system-secretary.txt")
    private Resource systemPrompt;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 @Value("${KAKAO_API_KEY}") String apiKey,
                                 @Value("${SLACK_TOKEN}") String token) {
        return builder
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(vectorStore).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .defaultTools(dateTimeTools, fileSystemTools, internetSearchTools, navigationTools, slackTools)
                .defaultToolContext(Map.of("apiKey", apiKey, "token", token))
                .defaultSystem(systemPrompt)
                .build();
    }
}
