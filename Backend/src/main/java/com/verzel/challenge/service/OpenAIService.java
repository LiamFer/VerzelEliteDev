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

    /**
     * Envia uma pergunta para o assistente da OpenAI e retorna a resposta estruturada.
     * <p>
     * Este método constrói um prompt detalhado para o modelo de IA, incluindo o estado atual do lead,
     * o fluxo de conversa obrigatório e as regras de negócio. Ele gerencia o contexto da conversa
     * usando o {@code previousResponseId}.
     * @param previousResponseId O ID da resposta anterior da IA, para manter o contexto da conversa. Pode ser nulo.
     * @param question A pergunta/mensagem atual do usuário.
     * @param lead O objeto {@link Lead} com os dados atuais do potencial cliente.
     * @return Um {@link AIResponseDTO} contendo a mensagem para o usuário, os dados do lead atualizados pela IA e a ação recomendada.
     */
    public AIResponseDTO askAssistant(String previousResponseId,String question, Lead lead){
        String leadJson = buildJson(lead);
        String systemPrompt = """
            Você é um SDR virtual natural e consultivo da empresa Atlas, responsável por atender potenciais clientes interessados em um CRM de fornecedores.
            === INFORMAÇÕES ATUAIS DO LEAD ===
            %s
            === FLUXO OBRIGATÓRIO (SIGA PASSO A PASSO) ===
            1. Apresente-se brevemente como Assistente Digital da Atlas e explique o serviço da Empresa, que ajuda a controlar Fornecedores com sua Solução de CRM.
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
                 1. Apresente-se brevemente como Assistente Digital da Atlas e explique o serviço da Empresa, que ajuda a controlar Fornecedores com sua Solução de CRM.
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

    /**
     * Converte um objeto genérico para uma string JSON.
     *
     * @param value O objeto a ser convertido.
     * @param <T> O tipo do objeto.
     * @return Uma representação em string JSON do objeto.
     * @throws RuntimeException se ocorrer um erro durante a serialização.
     */
    private <T> String buildJson(T value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter objeto para JSON", e);
        }
    }
}
