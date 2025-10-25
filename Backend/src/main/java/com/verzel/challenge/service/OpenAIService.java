package com.verzel.challenge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verzel.challenge.dto.chat.AIResponseBodyDTO;
import com.verzel.challenge.dto.chat.AIResponseDTO;
import com.verzel.challenge.dto.pipefy.Lead;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIService {
    private final WebClient webClient;

    public OpenAIService(@Value("${openai.token}") String openAIToken) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/responses")
                .defaultHeader("Authorization", "Bearer " + openAIToken)
                .build();
    }

    public AIResponseDTO askAssistant(String previousResponseId,String question, Lead lead){
        String leadJson = buildJson(lead);
        String systemPrompt = "Você é um SDR virtual da empresa Atlas, responsável por atender potenciais clientes interessados em um CRM de fornecedores." +
                "**IMPORTANTE:** Em nenhum momento pergunte datas ou horários que ele deseja agendar; **nunca** faça isso. Apenas confirme interesse (sim ou não)." +
                "Você deve conduzir a conversa de forma natural, sempre coletando informações do lead antes de perguntar sobre interesse em agendar uma reunião com o time." +
                "Atualmente, você já tem as seguintes informações do cliente: " + leadJson + ". " +
                "Seu objetivo é preencher todos os campos do lead, na ordem de prioridade: nome, email, empresa, necessidade. " +
                "Somente após todos os campos estarem preenchidos, pergunte se o lead tem interesse em agendar uma conversa com o time, definindo 'interesse' como true se ele demonstrar interesse explícito. " +
                "Nunca force o agendamento, e encerre a conversa casualmente se o lead não demonstrar interesse. " +
                "A sequência de abordagem deve ser: \n" +
                "Você deve seguir rigorosamente esta ordem de abordagem:\n" +
                "1. Apresente-se e explique brevemente o serviço.\n" +
                "2. Pergunte pelo nome e email do cliente se ainda não estiver preenchido.\n" +
                "3. Pergunte pela empresa do cliente se ainda não estiver preenchida.\n" +
                "4. Pergunte pela necessidade ou dor principal do cliente (ex.: controle de contratos, avaliação de desempenho, integração, rastreamento de entregas) se ainda não estiver preenchida.\n" +
                "5. Somente depois de todos os campos acima estarem preenchidos, pergunte se o cliente tem interesse em agendar uma reunião: \"Você gostaria de conversar com nosso time para iniciar o projeto ou adquirir o produto?\"" +
                "De EXTREMA IMPORTANCIA Não pergunte horários, apenas confirme interesse. " +
                "Você deve responder **EXCLUSIVAMENTE E SEMPRE** em JSON no formato: { 'mensagem': 'texto para o usuário', 'lead': { 'nome': string|null, 'email': string|null, 'empresa': string|null, 'necessidade': string|null, 'interesse': boolean|null } }";

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("input", question);

        if(previousResponseId!=null){
            body.put("instructions", "Você deve responder **EXCLUSIVAMENTE** em JSON no formato:" +
                    "{ 'mensagem': 'texto para o usuário', 'lead': { 'nome': string|null, 'email': string|null, 'empresa': string|null, 'necessidade': string|null, 'interesse': boolean|null } }");
            body.put("previous_response_id",previousResponseId);
        } else {
            body.put("instructions", systemPrompt);
        }

        String bodyJson = buildJson(body);
        AIResponseBodyDTO response = webClient.post()
                .header("Content-Type", "application/json")
                .bodyValue(bodyJson)
                .retrieve()
                .bodyToMono(AIResponseBodyDTO.class)
                .block();

        return response.getResponse();
    }

    private <T> String buildJson(T value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter objeto para JSON", e);
        }
    }
}
