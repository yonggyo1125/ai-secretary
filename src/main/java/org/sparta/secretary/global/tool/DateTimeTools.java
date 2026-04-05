package org.sparta.secretary.global.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DateTimeTools {
    @Tool(description = "현재 날짜와 시간 정보를 제공합니다.")
    public String getCurrentDateTime() {
        String nowTime = LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();

        log.info("현재 시간: {}", nowTime);
        return nowTime;
    }
}
