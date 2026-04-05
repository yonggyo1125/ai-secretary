package org.sparta.secretary.global.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.sparta.secretary.global.tool.dto.Coordinates;
import org.sparta.secretary.global.tool.dto.Routes;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NavigationTools {

    private final WebClient webClient;

    public NavigationTools(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 응답 버퍼 제한을 10M로 조정
                .build();
    }

    @Tool(description = "주소를 위도(latitude), 경도(longitude) 좌표로 변환합니다.")
    public Coordinates toCoordinates(@ToolParam(description = "변환할 지번 주소 또는 도로명 주소 (예: 서울특별시 강남구 테헤란로 427)") String address, ToolContext context) {
        try {
            if (!StringUtils.hasText(address)) {
                throw new IllegalArgumentException("변환할 주소가 비어있습니다.");
            }

            Object apiKeyObj = context.getContext().get("apiKey");
            if (apiKeyObj == null) {
                throw new IllegalArgumentException("주소 -> 좌표 변환 실패: Context에 apikey 존재하지 않음");
            }

            String apiKey = apiKeyObj.toString();
            ResponseEntity<JsonNode> res = this.webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("dapi.kakao.com")
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build(java.time.Duration.ofSeconds(10))
                    )
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block();
            if (res != null && res.getStatusCode().is2xxSuccessful()) {
                JsonNode body = res.getBody();
                if (body != null && body.has("documents") && !body.get("documents").isEmpty()) {
                    JsonNode document = body.get("documents").get(0);

                    // x = 경도(longitude), y = 위도(latitude)
                    double longitude = document.get("x").asDouble();
                    double latitude = document.get("y").asDouble();

                    log.info("주소 변환 성공: {} -> [Lat: {}, Lng: {}]", address, latitude, longitude);
                    return new Coordinates(latitude, longitude);
                }
            }

            // 검색 결과가 없는 경우
            log.warn("해당 주소에 대한 검색 결과가 없습니다: {}", address);
            return null;

        } catch (Exception e) {
            log.error("카카오 API 호출 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "여러 주소를 방문하는 최적의 경로와 예상 시간을 산출합니다. 입력된 주소들 중 가장 효율적인 동선을 구성합니다.")
    public List<Routes> calculateRoutes(@ToolParam(description = "방문할 주소 목록") List<String> addresses, ToolContext context) {
        try {
            if (addresses == null || addresses.size() < 2) {
                throw new IllegalArgumentException("경로 산출을 위해서는 최소 2개 이상의 주소가 필요합니다.");
            }

            Object apiKeyObj = context.getContext().get("apiKey");
            if (apiKeyObj == null) {
                throw new IllegalArgumentException("경로 산출 실패: Context에 apiKey 존재하지 않습니다.");
            }
            String apiKey = apiKeyObj.toString();

            // 모든 주소를 좌표로 변환
            List<Coordinates> coords = addresses.stream()
                    .map(addr -> toCoordinates(addr, context))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            if (coords.size() < 2) return List.of();

            // 요청 데이터 가공 (origin, destination, waypoints)
            Map<String, Object> params = new java.util.HashMap<>();
            Coordinates departure = coords.get(0);
            Coordinates arrival = coords.get(coords.size() - 1);

            params.put("origin", Map.of("name", "출발지", "x", departure.longitude(), "y", departure.latitude()));
            params.put("destination", Map.of("name", "도착지", "x", arrival.longitude(), "y", arrival.latitude()));

            if (coords.size() > 2) {
                List<Map<String, Object>> waypoints = coords.subList(1, coords.size() - 1).stream()
                        .map(c -> {
                            Map<String, Object> item = new java.util.HashMap<>();
                            item.put("name", "경유지");
                            item.put("x", c.longitude());
                            item.put("y", c.latitude());
                            return item;
                        })
                        .toList();
                params.put("waypoints", waypoints);
            }

            ResponseEntity<JsonNode> response = this.webClient.post()
                    .uri("https://apis-navi.kakaomobility.com/v1/waypoints/directions")
                    .header("Authorization", "KakaoAK " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(params)
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block(java.time.Duration.ofSeconds(10));

            // 응답 처리 및 전체 방문지 리스트 가공
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                JsonNode nodes = response.getBody();
                JsonNode routesNode = nodes.get("routes");

                if (routesNode != null && routesNode.isArray() && !routesNode.isEmpty()) {
                    JsonNode summary = routesNode.get(0).get("summary");
                    double totalDistance = summary.get("distance").asDouble(0.0);
                    double totalDuration = summary.get("duration").asDouble(0.0);

                    log.info("경로 산출 성공: 총 거리 {}km, 총 소요시간 {}분", totalDistance / 1000.0, totalDuration / 60.0);

                    // 모든 방문지를 담을 리스트 생성
                    List<Routes> resultList = new java.util.ArrayList<>();

                    for (int i = 0; i < addresses.size(); i++) {
                        String currentAddr = addresses.get(i);
                        Coordinates currentCoord = coords.get(i);

                        if (i == 0) {
                            // 출발지
                            resultList.add(new Routes(currentAddr, currentCoord, java.time.LocalDateTime.now(), 0, "출발지"));
                        } else if (i == addresses.size() - 1) {
                            // 최종 목적지 (전체 시간/거리 정보 포함)
                            resultList.add(new Routes(
                                    currentAddr,
                                    currentCoord,
                                    java.time.LocalDateTime.now().plusSeconds((long)totalDuration),
                                    (int)(totalDuration / 60),
                                    String.format("최종 목적지 (총 거리: %.2fkm)", totalDistance / 1000.0)
                            ));
                        } else {
                            // 중간 경유지
                            resultList.add(new Routes(currentAddr, currentCoord, null, 0, "경유지 " + i));
                        }
                    }
                    return resultList;
                }
            }
            return List.of();

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("카카오 API 에러 바디: {}", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("경로 산출 중 예외 발생: {}", e.getMessage());
            throw e;
        }
    }
}
