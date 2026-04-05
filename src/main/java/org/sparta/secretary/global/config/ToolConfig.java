package org.sparta.secretary.global.config;

import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {
    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }
}
