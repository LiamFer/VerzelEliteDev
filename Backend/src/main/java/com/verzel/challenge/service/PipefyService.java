package com.verzel.challenge.service;

import com.verzel.challenge.dto.pipefy.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class PipefyService {

    @Value("${pipefy.token}")
    private String pipefyToken;

    @Value("${pipefy.pipe.id}")
    private String pipefyPipeId;

    private WebClient webClient;
    private Map<String, String> phaseMap = new HashMap<>();
    private Map<String, String> fieldMap = new HashMap<>();

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.pipefy.com/graphql")
                .defaultHeader("Authorization", "Bearer " + pipefyToken)
                .build();
        loadPhases();
        loadFields();
    }

    // Minha classe Genérica para Requests
    public <T> T performRequest(String query, Class<T> responseType) {
        return webClient.post()
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private void loadPhases() {
        String query = String.format("""
            query {
              pipe(id: %s) {
                phases {
                  id
                  name
                }
              }
            }
        """, pipefyPipeId);

        try {
            PhaseResponse response = performRequest(query,PhaseResponse.class);
            if (response != null && response.data != null && response.data.pipe != null) {
                for (PhaseResponse.Phase p : response.data.pipe.phases) {
                    phaseMap.put(p.name, p.id);
                }
            }

            System.out.println("Phases carregadas: " + phaseMap);
        } catch (Exception e) {
            System.err.println("Erro ao carregar phases: " + e.getMessage());
        }
    }
    private void loadFields() {
        String query = String.format("""
            query {
              pipe(id: %s) {
                start_form_fields {
                  id
                  label
                }
              }
            }
        """, pipefyPipeId);

        try {
            FieldResponse response = performRequest(query,FieldResponse.class);
            if (response != null && response.data != null && response.data.pipe != null) {
                for (PipefyField f : response.data.pipe.startFormFields) {
                    fieldMap.put(f.label, f.id);
                }
            }
            System.out.println("Fields carregados: " + fieldMap);
        } catch (Exception e) {
            System.err.println("Erro ao carregar fields: " + e.getMessage());
        }
    }


    public String createCard(String nome, String email, String company, String necessidade, boolean interesse, String meetingLink) {
        String phaseId = phaseMap.get("Novo Lead");
        if (phaseId == null) throw new IllegalStateException("Fase 'Novo Lead' não encontrada.");

        Optional<Card> alreadyExists = this.getCardByEmail(email);
        if(alreadyExists.isPresent()) return alreadyExists.get().id;

        String nomeId = fieldMap.get("Nome");
        String emailId = fieldMap.get("E-mail");
        String companyId = fieldMap.get("Empresa");
        String necessidadeId = fieldMap.get("Necessidade");
        String meetingId = fieldMap.get("Link da Reunião");
        String interesseId = fieldMap.get("Interessado");

        String mutation = String.format("""
            mutation {
              createCard(input: {
                pipe_id: %s,
                phase_id: %s,
                title: "Lead capturado via API",
                fields_attributes: [
                  { field_id: "%s", field_value: "%s" },
                  { field_id: "%s", field_value: "%s" },
                  { field_id: "%s", field_value: "%s" },
                  { field_id: "%s", field_value: "%s" },
                  { field_id: "%s", field_value: "%s" },
                  { field_id: "%s", field_value: "%s" }
                ]
              }) {
                card { id title }
              }
            }
        """, pipefyPipeId, phaseId,
            nomeId, nome,
            emailId, email,
            companyId, company,
            necessidadeId, necessidade,
            meetingId, meetingLink,
            interesseId, interesse ? "Sim" : "Não");
        return performRequest(mutation,String.class);
    }

    private Optional<Card> getCardByEmail(String email) {
        String emailFieldId = fieldMap.get("E-mail");
        if (emailFieldId == null) throw new IllegalStateException("Campo 'E-mail' não encontrado.");

        String query = String.format("""
            query {
              findCards(pipeId: %s, search: {fieldId: "%s", fieldValue: "%s"}) {
                edges {
                  node {
                    id
                    title
                    fields {
                      name
                      value
                    }
                  }
                }
              }
            }
        """, pipefyPipeId, emailFieldId, email);

        try {
            FindCardResponse response = performRequest(query, FindCardResponse.class);
            if (response != null &&
                    response.data != null &&
                    response.data.findCards != null &&
                    !response.data.findCards.edges.isEmpty()) {
                return Optional.of(response.data.findCards.edges.getFirst().node);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar card: " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean updateCardFields(String cardId, boolean interesse, String meetingLink) {
        String interesseId = fieldMap.get("Interessado");
        String meetingId = fieldMap.get("Link da Reunião");

        String mutation = String.format("""
                    mutation {
                      updateFieldsValues(input:{
                        nodeId: "%s"
                        values:[{fieldId: "%s", value:"%s"},
                                {fieldId: "%s", value:"%s"}
                        ]
                      })
                      {
                        success
                      }
                    }
                """,cardId,
                meetingId,meetingLink,
                interesseId,interesse ? "Sim" : "Não");

        UpdateFieldsResponse response = performRequest(mutation, UpdateFieldsResponse.class);
        return response != null &&
                response.data != null &&
                response.data.updateFieldsValues != null &&
                response.data.updateFieldsValues.success;
    }
}
