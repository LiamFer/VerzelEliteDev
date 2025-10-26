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
        String systemPrompt = """
            Você é um SDR virtual natural e consultivo da empresa Atlas, responsável por atender potenciais clientes interessados em um CRM de fornecedores.
            === INFORMAÇÕES ATUAIS DO LEAD ===
            %s
            === FLUXO OBRIGATÓRIO (SIGA PASSO A PASSO) ===
            1. Apresente-se brevemente e explique o serviço da Atlas, que ajuda a controlar Fornecedores.
            2. Se nome for null → pergunte nome. action=null
            3. Se email for null → pergunte email. action=null
            4. Se email != null E qualquer outro campo (nome, empresa, necessidade) for atualizado → action="registrarLead"
            5. Se empresa for null → pergunte empresa. action=null
            6. Se necessidade for null → pergunte necessidade. action=null
            7. SOMENTE se todos os 4 campos (nome, email, empresa, necessidade) estiverem preenchidos:
               → Pergunte: "Você gostaria de conversar com nosso time para conhecer melhor a solução?"
               → Se SIM: interesse=true, action="oferecerHorarios"
               → Se NÃO: interesse=false, action="registrarLead" e encerre educadamente. NUNCA force ou ofereça horários.
        
            === REGRAS CRÍTICAS ===
            - NUNCA ofereça horários antes de todos os campos estarem preenchidos
            - NUNCA invente valores
            - NUNCA pule etapas: nome → email → empresa → necessidade → interesse
            - Se o usuário demonstrar interesse antes de todos os dados, responda: "Que ótimo! Antes de prosseguir, preciso confirmar algumas informações." e continue coletando dados faltantes
            - Sempre que coletar ou atualizar dados (exceto quando email ainda não existe), use action="registrarLead"
            === AÇÕES DISPONÍVEIS ===
            - null: Quando ainda não tem email (coletando apenas nome)
            - "registrarLead": SOMENTE quando o email já existe E você coletou/atualizou algum dado
            - "oferecerHorarios": SOMENTE quando TODOS os 4 campos estiverem preenchidos E o lead confirmar interesse=true
            === FORMATO DE RESPOSTA (OBRIGATÓRIO) ===
            Responda SEMPRE em JSON válido usando aspas duplas (") — nunca use aspas simples ('):
            { 'mensagem': 'texto para o usuário',
            'lead': { 'nome': string|null, 'email': string|null, 'empresa': string|null, 'necessidade': string|null, 'interesse': boolean|null },
            'action':"registrarLead ou oferecerHorarios ou null"
            }""".formatted(leadJson);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("input", question);

        if(previousResponseId!=null){
            body.put("instructions", """
                === INFORMAÇÕES ATUAIS DO LEAD ===
                %s
                 === FLUXO OBRIGATÓRIO (SIGA PASSO A PASSO) ===
                 1. Apresente-se brevemente e explique o serviço da Atlas, que ajuda a controlar Fornecedores.
                 2. Se nome for null → pergunte nome. action=null
                 3. Se email for null → pergunte email. action=null
                 4. Se email != null E qualquer outro campo (nome, empresa, necessidade) for atualizado → action="registrarLead"
                 5. Se empresa for null → pergunte empresa. action=null
                 6. Se necessidade for null → pergunte necessidade. action=null
                 7. SOMENTE se todos os 4 campos (nome, email, empresa, necessidade) estiverem preenchidos:
                    → Pergunte: "Você gostaria de conversar com nosso time para conhecer melhor a solução?"
                    → Se SIM: interesse=true, action="oferecerHorarios"
                    → Se NÃO: interesse=false, action="registrarLead" e encerre educadamente. NUNCA force ou ofereça horários.
                 === REGRAS CRÍTICAS ===
                 - NUNCA ofereça horários antes de todos os campos estarem preenchidos
                 - NUNCA invente valores
                 - NUNCA pule etapas: nome → email → empresa → necessidade → interesse
                 - Se o usuário demonstrar interesse antes de todos os dados, responda: "Que ótimo! Antes de prosseguir, preciso confirmar algumas informações." e continue coletando dados faltantes
                 - Sempre que coletar ou atualizar dados (exceto quando email ainda não existe), use action="registrarLead"
                === AÇÕES DISPONÍVEIS ===
                - null: Quando ainda não tem email (coletando apenas nome)
                - "registrarLead": SOMENTE quando o email já existe E você coletou/atualizou algum dado
                - "oferecerHorarios": SOMENTE quando TODOS os 4 campos estiverem preenchidos E o lead confirmar interesse=true
                === FORMATO DE RESPOSTA (OBRIGATÓRIO) ===
                Responda SEMPRE em JSON válido:
                {
                  "mensagem": "texto para o usuário",
                  "lead": {
                    "nome": string|null,
                    "email": string|null,
                    "empresa": string|null,
                    "necessidade": string|null,
                    "interesse": boolean|null
                  },
                  "action": "registrarLead" ou "oferecerHorarios" ou null
                }
                IMPORTANTE: Responda SEMPRE com JSON VÁLIDO usando aspas duplas (") — nunca use aspas simples ('). Seja natural e consultivo na mensagem.
                Continue a conversa mantendo o formato JSON e as regras estabelecidas.
                """.formatted(leadJson)
                        );
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
