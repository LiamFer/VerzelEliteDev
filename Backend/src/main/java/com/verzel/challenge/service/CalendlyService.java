package com.verzel.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CalendlyService {

    @Value("${calendly.token}")
    private String calendlyToken;
    private String userUri;
    private String eventTypeUri;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.calendly.com")
                .defaultHeader("Authorization", "Bearer " + calendlyToken)
                .build();
        getUserUri();
        getEventTypeUri();
    }

    public void getUserUri() {
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/me").build())
                .header("Authorization", "Bearer " + calendlyToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            this.userUri = root.path("resource").path("uri").asText();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair userUri do Calendly", e);
        }
    }

    public void getEventTypeUri() {
        // Usa o userUri para buscar os event types do usuário
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/event_types")
                        .queryParam("user", userUri)
                        .build())
                .header("Authorization", "Bearer " + calendlyToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode collection = root.path("collection");
            if (collection.isArray() && !collection.isEmpty()) {
                this.eventTypeUri = collection.get(0).path("uri").asText();
            } else {
                throw new RuntimeException("Nenhum EventType encontrado para o usuário.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair eventTypeUri do Calendly", e);
        }
    }

    public List<Map<String, Object>> getAvailableSlots() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
        OffsetDateTime end = start.plusDays(6);

        Map response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/event_type_available_times")
                        .queryParam("event_type", eventTypeUri)
                        .queryParam("start_time", start)
                        .queryParam("end_time", end)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> slots = (List<Map<String, Object>>) response.get("collection");
        List<Map<String, Object>> available = slots.stream()
                .filter(slot -> "available".equals(slot.get("status")))
                .toList();
        Collections.shuffle(available);
        return available.stream().limit(3).toList();
    }
}
