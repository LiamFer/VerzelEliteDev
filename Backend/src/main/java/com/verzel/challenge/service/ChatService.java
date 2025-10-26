package com.verzel.challenge.service;

import com.verzel.challenge.dto.calendly.WebhookPayload;
import com.verzel.challenge.dto.chat.AIResponseDTO;
import com.verzel.challenge.dto.chat.MessageDTO;
import com.verzel.challenge.dto.chat.ResponseDTO;
import com.verzel.challenge.dto.pipefy.Lead;
import com.verzel.challenge.entity.ChatSessionEntity;
import com.verzel.challenge.entity.LeadEntity;
import com.verzel.challenge.entity.MessageEntity;
import com.verzel.challenge.mapper.LeadMapper;
import com.verzel.challenge.repository.ChatSessionRepository;
import com.verzel.challenge.repository.LeadRepository;
import com.verzel.challenge.repository.MessageRepository;
import com.verzel.challenge.type.ActionAI;
import com.verzel.challenge.type.ResponseAction;
import com.verzel.challenge.type.Sender;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ChatService {
    private final OpenAIService openAIService;
    private final PipefyService pipefyService;
    private final CalendlyService calendlyService;
    private final ChatSessionRepository chatSessionRepository;
    private final LeadRepository leadRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(OpenAIService openAIService, PipefyService pipefyService, CalendlyService calendlyService, ChatSessionRepository chatSessionRepository, LeadRepository leadRepository, MessageRepository messageRepository, SimpMessagingTemplate messagingTemplate) {
        this.openAIService = openAIService;
        this.pipefyService = pipefyService;
        this.calendlyService = calendlyService;
        this.chatSessionRepository = chatSessionRepository;
        this.leadRepository = leadRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Processa a mensagem recebida do usuário, interage com a IA e executa as ações necessárias.
     *
     * @param userMessage A mensagem enviada pelo usuário.
     * @param sessionId   O ID da sessão de chat atual.
     * @return Um {@link ResponseDTO} contendo a resposta para o cliente.
     */
    public ResponseDTO handleMessage(MessageDTO userMessage, String sessionId){
        ChatSessionEntity chat = getChatBySessionId(sessionId);
        Lead lead = LeadMapper.toLead(chat.getLead());

        AIResponseDTO aiResponse = openAIService.askAssistant(chat.getPreviousResponseId(),userMessage.message(), lead);
        ResponseDTO response = handleAIAction(aiResponse,chat);
        storeMessages(chat,userMessage,aiResponse.getMensagem());

        return response;
    };

    /**
     * Manipula o webhook de agendamento de reunião (ex: do Calendly).
     * Atualiza o lead com o link da reunião no banco de dados e no Pipefy,
     * e notifica o usuário via WebSocket, se aplicável.
     * @param payload O payload recebido do webhook com os detalhes do agendamento.
     */
    public void scheduleMeeting(WebhookPayload payload){
        String email = payload.getInviteeEmail();
        String meetingLink = payload.getMeetingLink();
        LeadEntity lead = leadRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("Lead não encontrado no Webhook"));
        boolean differentMeetingLink = !Objects.equals(lead.getMeetingLink(), meetingLink);

        lead.setMeetingLink(meetingLink);
        leadRepository.save(lead);
        pipefyService.updateCardMeetingFields(
                lead.getCardId(),
                meetingLink,
                payload.getScheduled_event().getStart_time()
        );

        ChatSessionEntity lastChat = chatSessionRepository
                .findFirstByLeadIdOrderByLastInteractionDesc(lead.getId())
                .orElse(null);

        if (lastChat != null && differentMeetingLink) {
            ResponseDTO response = new ResponseDTO(
                    ResponseAction.talk,
                    "Aqui está o link da nossa reunião " + meetingLink + ", aguardamos você lá!",
                    ""
            );
            messagingTemplate.convertAndSend("/topic/" + lastChat.getSessionId(), response);
        }
    }

    /**
     * Trata a ação recomendada pela IA.
     *
     * @param response A resposta da IA, contendo a ação a ser executada.
     * @param chat     A entidade da sessão de chat atual.
     * @return Um {@link ResponseDTO} formatado de acordo com a ação da IA.
     */
    private ResponseDTO handleAIAction(AIResponseDTO response, ChatSessionEntity chat) {
        chat.setPreviousResponseId(response.getId());
        chatSessionRepository.save(chat);

        if (response.getAction() == null) return new ResponseDTO(ResponseAction.talk, response.getMensagem(), "");

        switch (response.getAction()) {
            case ActionAI.registrarLead -> {
                createOrUpdateLead(response, chat);
                return new ResponseDTO(ResponseAction.talk, response.getMensagem(), "");
            }
            case ActionAI.oferecerHorarios -> {
                createOrUpdateLead(response,chat);
                return new ResponseDTO(ResponseAction.offer, response.getMensagem(), calendlyService.getAvailableSlots(response.getLead()));
            }
            default -> {
                return new ResponseDTO(ResponseAction.talk, response.getMensagem(), "");
            }
        }

    }

    /**
    * Cria um novo lead ou atualiza um existente com base na resposta da IA.
    * Sincroniza as informações com o banco de dados local e com o Pipefy.
    *
    * @param response A resposta da IA contendo os dados do lead.
    * @param chat     A entidade da sessão de chat para associar ao lead.
    */
    private void createOrUpdateLead(AIResponseDTO response, ChatSessionEntity chat) {
        LeadEntity databaseLead = chat.getLead();
        Lead assistantLead = response.getLead();

        // Criação de novo lead se não existir e tiver email
        if (databaseLead == null && assistantLead.getEmail() != null) {
            databaseLead = criarOuRecuperarLead(assistantLead.getEmail());
            associarLeadAoChat(chat, databaseLead);
        }

        // Atualização do lead existente
        if (databaseLead != null) {
            atualizarDadosDoLead(databaseLead, assistantLead);
        }
    }

    /**
    * Cria um novo lead ou recupera um existente pelo email.
    * Se o lead não existir, cria um card no Pipefy.
    */
    private LeadEntity criarOuRecuperarLead(String email) {
        return leadRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> criarNovoLeadComCard(email));
    }

    /**
    * Cria um novo lead no banco e no Pipefy.
    */
    private LeadEntity criarNovoLeadComCard(String email) {
        LeadEntity newLead = new LeadEntity(email);
        String cardId = pipefyService.createCardWithEmail(email);
        newLead.setCardId(cardId);
        return leadRepository.save(newLead);
    }

    /**
    * Associa o lead à sessão de chat.
    */
    private void associarLeadAoChat(ChatSessionEntity chat, LeadEntity lead) {
        chat.setLead(lead);
        chatSessionRepository.save(chat);
    }

    /**
    * Atualiza os dados do lead no banco e no Pipefy.
    */
    private void atualizarDadosDoLead(LeadEntity databaseLead, Lead assistantLead) {
        databaseLead.setName(assistantLead.getNome());
        databaseLead.setEmail(assistantLead.getEmail());
        databaseLead.setCompany(assistantLead.getEmpresa());
        databaseLead.setNecessity(assistantLead.getNecessidade());
        databaseLead.setInterested(assistantLead.getInteresse());
        
        pipefyService.updateCardFields(
            databaseLead.getCardId(),
            assistantLead.getNome(),
            assistantLead.getEmail(),
            assistantLead.getEmpresa(),
            assistantLead.getNecessidade(),
            assistantLead.getInteresse()
        );
        
        leadRepository.save(databaseLead);
    }

    /**
     * Armazena as mensagens do usuário e do assistente no banco de dados.
     *
     * @param chat             A sessão de chat à qual as mensagens pertencem.
     * @param userMessage      A mensagem original do usuário.
     * @param assistantMessage A resposta gerada pelo assistente.
     */
    private void storeMessages(ChatSessionEntity chat, MessageDTO userMessage,String assistantMessage){
        messageRepository.save(new MessageEntity(Sender.USER, userMessage.message(),chat));
        messageRepository.save(new MessageEntity(Sender.ASSISTANT,assistantMessage,chat));
    }

    /**
     * Recupera uma sessão de chat existente pelo ID da sessão ou cria uma nova se não existir.
     *
     * @param sessionId O ID da sessão a ser buscada ou criada.
     * @return A entidade {@link ChatSessionEntity} correspondente.
     */
    private ChatSessionEntity getChatBySessionId(String sessionId){
        ChatSessionEntity chat;
        Optional<ChatSessionEntity> chatSession = chatSessionRepository.findBySessionId(sessionId);
        if(chatSession.isEmpty()){
            chat = new ChatSessionEntity(sessionId);
            chat = chatSessionRepository.save(chat);
        } else {
            chat = chatSession.get();
        }
        return chat;
    }




}
