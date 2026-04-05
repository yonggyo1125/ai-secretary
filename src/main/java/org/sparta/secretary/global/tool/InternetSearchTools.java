package org.sparta.secretary.global.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class InternetSearchTools {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public InternetSearchTools(@Value("${BRAVE_SEARCH_API_KEY}") String apiKey, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        String searchEndpoint = UriComponentsBuilder.fromUri(URI.create("https://serpapi.com/search.json"))
                .queryParam("engine", "google")
                .queryParam("location", "South Korea")
                .queryParam("google_domain", "google.co.kr")
                .queryParam("hl", "ko")
                .queryParam("gl", "kr")
                .queryParam("api_key", apiKey)
                .build()
                .toString();

        this.webClient = webClientBuilder.baseUrl(searchEndpoint)
                .baseUrl(searchEndpoint)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Tool(description = "인터넷 검색을 합니다. 제목, 링크, 요약을 문자열로 반환합니다.")
    public String googleSearch(String query) {
        try {
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("organic_results");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                JsonNode item = items.get(i);
                String title = item.path("title").asText();
                String link = item.path("link").asText();
                String snippet = item.path("snippet").asText();
                sb.append(String.format("[%d] %s\n%s\n%s\n\n", i + 1, title, link, snippet));
            }
            return sb.toString().trim();

        } catch (Exception e) {
            return "인터넷 검색 중 오류 발생: " + e.getMessage();
        }
    }

    @Tool(description = "웹 페이지의 본문 텍스트를 반환합니다.")
    public String fetchPageContent(String url) {
        try {
            // WebClient를 사용해 응답 HTML 가져오기
            String html = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (!StringUtils.hasText(html)) {
                return "페이지 내용을 가져올 수 없습니다.";
            }

            // Jsoup으로 파싱하고 <body> 내부 텍스트 추출
            Document doc = Jsoup.parse(html);
            String bodyText = doc.body().text();

            return StringUtils.hasText(bodyText) ? bodyText : "본문 텍스트가 비어 있습니다.";

        } catch (Exception e) {
            return "페이지 로딩 중 오류 발생: " + e.getMessage();
        }
    }
}
