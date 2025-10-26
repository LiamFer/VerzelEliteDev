package com.verzel.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verzel.challenge.dto.pipefy.Lead;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class CalendlyService {

    @Value("${calendly.token}")
    private String calendlyToken;
    @Value("${calendly.callback}")
    private String calendlyCallback;
    private String organizationUri;
    private String userUri;
    private String eventTypeUri;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.calendly.com")
                .defaultHeader("Authorization", "Bearer " + calendlyToken)
                .build();

        getUris();
        getEventTypeUri();
        createWebhook(calendlyCallback);

        System.out.println("ORGANIZATION URI -> " + organizationUri);
    }

    private void createWebhook(String callbackUrl) {
        Map<String, Object> body = Map.of(
                "url", callbackUrl,
                "events", List.of("invitee.created"),
                "organization", this.organizationUri,
                "scope", "organization"
        );
        try {
            webClient.post()
                    .uri("/webhook_subscriptions")
                    .header("Authorization", "Bearer " + calendlyToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                System.out.println("Webhook do Calendly já existe, ignorando...");
            }
        }
    }

    private void getUris() {
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
            this.organizationUri = root.path("resource").path("current_organization").asText();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair userUri do Calendly", e);
        }
    }

    private void getEventTypeUri() {
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

    public List<Map<String, Object>> getAvailableSlots(Lead lead) {
        int offset = 0;
        List<Map<String, Object>> slots = Collections.emptyList();

        while (slots.size() < 2) {
            OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7 + offset);
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
            List<Map<String, Object>> allSlots = (List<Map<String, Object>>) response.get("collection");
            if (allSlots != null) {
                slots = allSlots;
            }
            offset++;
        }

        Collections.shuffle(slots);
        return slots.stream().limit(3).map(slot -> {
            // Adiciono os Dados do Usuário pra ele só ter que clicar em Schedule
            String originalUrl = (String) slot.get("scheduling_url");
            String urlComParams = UriComponentsBuilder.fromUriString(originalUrl)
                    .queryParam("name", lead.getNome())
                    .queryParam("email", lead.getEmail())
                    .build()
                    .toUriString();
            Map<String, Object> slotModificado = new HashMap<>(slot);
            slotModificado.put("scheduling_url", urlComParams);
            return slotModificado;
        }).toList();
    }

}
