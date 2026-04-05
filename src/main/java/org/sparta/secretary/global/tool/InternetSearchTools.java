package org.sparta.secretary.global.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;

@Slf4j
@Component
public class InternetSearchTools {
    private final WebClient webClient;
    private final String serpApiKey;
    private final ObjectMapper objectMapper;

    public InternetSearchTools(@Value("${BRAVE_SEARCH_API_KEY}") String apiKey, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.serpApiKey = apiKey;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
    }

    @Tool(description = "인터넷 검색을 합니다. 제목, 링크, 요약을 문자열로 반환합니다.")
    public String googleSearch(String query) {
        log.info("구글 검색 실행: {}", query);
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("serpapi.com")
                            .path("/search.json")
                            .queryParam("engine", "google")
                            .queryParam("q", query)
                            .queryParam("location", "South Korea")
                            .queryParam("google_domain", "google.co.kr")
                            .queryParam("hl", "ko")
                            .queryParam("gl", "kr")
                            .queryParam("api_key", serpApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("organic_results");

            if (items.isMissingNode() || items.isEmpty()) {
                return "검색 결과가 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                JsonNode item = items.get(i);
                sb.append(String.format("[%d] %s\n%s\n%s\n\n",
                        i + 1,
                        item.path("title").asText(),
                        item.path("link").asText(),
                        item.path("snippet").asText()));
            }
            return sb.toString().trim();

        } catch (Exception e) {
            log.error("검색 에러: {}", e.getMessage());
            return "인터넷 검색 중 오류 발생: " + e.getMessage();
        }
    }

    @Tool(description = "웹 페이지의 본문 텍스트를 반환합니다. 검색 결과의 링크 내용을 자세히 읽을 때 사용합니다.")
    public String fetchPageContent(String url) {
        log.info("페이지 내용 추출 시도: {}", url);
        try {
            // 외부 사이트 접속 시에는 완전한 URL을 그대로 사용합니다.
            String html = webClient.get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(10)); // 타임아웃 추가

            if (!StringUtils.hasText(html)) return "내용이 비어있습니다.";

            Document doc = Jsoup.parse(html);
            // 스크립트나 스타일 태그 제거 후 텍스트만 추출
            doc.select("script, style, nav, footer").remove();
            String bodyText = doc.body().text();

            // 너무 길면 LLM 토큰 문제가 생기므로 일부 절삭 (예: 3000자)
            return bodyText.length() > 3000 ? bodyText.substring(0, 3000) + "..." : bodyText;

        } catch (Exception e) {
            log.error("페이지 로딩 에러: {}", e.getMessage());
            return "페이지 로딩 실패: " + e.getMessage();
        }
    }
}