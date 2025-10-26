package com.verzel.challenge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verzel.challenge.dto.pipefy.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    /**
     * Executa uma requisição GraphQL genérica para a API do Pipefy.
     *
     * @param query A string da query ou mutation GraphQL a ser executada.
     * @param responseType A classe do DTO esperado como resposta.
     * @param <T> O tipo do DTO de resposta.
     * @return O objeto de resposta desserializado.
     */
    public <T> T performRequest(String query, Class<T> responseType) {
        return webClient.post()
                .bodyValue(Map.of("query", query))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    // Funções de Configuração do Pipefy
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

    /**
     * Cria um card no Pipefy com o e-mail do lead.
     * <p>
     * Este método é idempotente. Se um card com o e-mail fornecido já existir, ele retorna o ID do card existente.
     * Caso contrário, cria um novo card na fase "Pré-Vendas".
     * @param email O e-mail do lead para criar o card.
     * @return O ID do card do Pipefy, seja ele novo ou existente.
     * @throws IllegalStateException se a fase "Pré-Vendas" não for encontrada na configuração do pipe.
     */
    public String createCardWithEmail(String email) {
        String phaseId = phaseMap.get("Pré-Vendas");
        if (phaseId == null) throw new IllegalStateException("Fase 'Pré-Vendas' não encontrada.");

        Optional<Card> alreadyExists = this.getCardByEmail(email);
        if(alreadyExists.isPresent()) return alreadyExists.get().id;

        String emailId = fieldMap.get("E-mail");

        String mutation = String.format("""
            mutation {
              createCard(input: {
                pipe_id: %s,
                phase_id: %s,
                title: "Lead capturado via API",
                fields_attributes: [
                  { field_id: "%s", field_value: "%s" }
                ]
              }) {
                card { id title }
              }
            }
        """, pipefyPipeId, phaseId,
                emailId, email);
        String response = performRequest(mutation,String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("data").path("createCard").path("card").path("id").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao extrair ID do Card Criado", e);
        }
    }

    /**
     * Atualiza os campos de um card existente no Pipefy.
     * <p>
     * Se o interesse for definido como {@code false}, este método também limpará os campos de agendamento de reunião.
     * @param cardId O ID do card a ser atualizado.
     * @param nome O nome do lead.
     * @param email O e-mail do lead.
     * @param company A empresa do lead.
     * @param necessidade A necessidade do lead.
     * @param interesse O status de interesse do lead.
     * @return {@code true} se a atualização for bem-sucedida, {@code false} caso contrário.
     */
    public boolean updateCardFields(String cardId, String nome, String email, String company, String necessidade, Boolean interesse) {
        String nomeId = fieldMap.get("Nome");
        String emailId = fieldMap.get("E-mail");
        String companyId = fieldMap.get("Empresa");
        String necessidadeId = fieldMap.get("Necessidade");
        String interesseId = fieldMap.get("Interessado");

        String interesseValue = "null";
        if (Boolean.TRUE.equals(interesse)) {
            interesseValue = "\"Sim\"";
        } else if (Boolean.FALSE.equals(interesse)){
            interesseValue = "\"Não\"";
        }

        String mutation = String.format("""
                    mutation {
                      updateFieldsValues(input:{
                        nodeId: "%s"
                        values:[{ fieldId: "%s", value: "%s" },
                              { fieldId: "%s", value: "%s" },
                              { fieldId: "%s", value: "%s" },
                              { fieldId: "%s", value: "%s" },
                              { fieldId: "%s", value: %s }]
                      })
                      {
                        success
                      }
                    }
                """,cardId,
                nomeId, checkIfNull(nome),
                emailId, checkIfNull(email),
                companyId, checkIfNull(company),
                necessidadeId, checkIfNull(necessidade),
                interesseId, interesseValue
                );

        // Caso ele não queira mais eu limpo o campo da Reunião
        if(Boolean.FALSE.equals(interesse)){
            updateCardMeetingFields(cardId,"",null);
        }

        UpdateFieldsResponse response = performRequest(mutation, UpdateFieldsResponse.class);
        return response != null &&
                response.data != null &&
                response.data.updateFieldsValues != null &&
                response.data.updateFieldsValues.success;
    }

    /**
     * Atualiza os campos específicos de agendamento de reunião em um card do Pipefy.
     *
     * @param cardId O ID do card a ser atualizado.
     * @param meetingLink O link da reunião agendada.
     * @param meetingTimeUtc A data e hora da reunião em formato UTC (String).
     */
    public void updateCardMeetingFields(String cardId,String meetingLink, String meetingTimeUtc) {
        String meetingId = fieldMap.get("Link da Reunião");
        String meetingTimeId = fieldMap.get("Hora da Reunião");
        String meetingTimeValue = "null";
        if (meetingTimeUtc != null) {
            Instant utcInstant = Instant.parse(meetingTimeUtc);
            ZonedDateTime spTime = utcInstant.atZone(ZoneId.of("America/Sao_Paulo"));
            meetingTimeValue = "\"" + spTime.toLocalDateTime().toString() + "\"";
        }
        String mutation = String.format("""
                    mutation {
                      updateFieldsValues(input:{
                        nodeId: "%s"
                        values:[
                              { fieldId: "%s", value: "%s" },
                              { fieldId: "%s", value: %s }]
                      })
                      {
                        success
                      }
                    }
                """,cardId,
                meetingId, meetingLink,
                meetingTimeId, meetingTimeValue
        );
        UpdateFieldsResponse response = performRequest(mutation, UpdateFieldsResponse.class);
    }

    /**
     * Busca um card no Pipefy pelo endereço de e-mail.
     *
     * @param email O e-mail a ser pesquisado.
     * @return Um {@link Optional} contendo o {@link Card} se encontrado, ou um Optional vazio caso contrário.
     * @throws IllegalStateException se o campo "E-mail" não for encontrado na configuração do pipe.
     */
    public Optional<Card> getCardByEmail(String email) {
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

        FindCardResponse response = performRequest(query, FindCardResponse.class);
        if (response != null &&
                response.data != null &&
                response.data.findCards != null &&
                !response.data.findCards.edges.isEmpty()) {
            return Optional.of(response.data.findCards.edges.getFirst().node);
        } else {
            return Optional.empty();
        }
    }

    private String checkIfNull(String value){
        if(value == null){
            return "";
        }
        if (value.equalsIgnoreCase("null")){
            return "";
        }
        return value;
    }

}
