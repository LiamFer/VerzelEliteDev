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

    public ResponseDTO handleMessage(MessageDTO userMessage, String sessionId){
        ChatSessionEntity chat = getChatBySessionId(sessionId);
        Lead lead = LeadMapper.toLead(chat.getLead());

        AIResponseDTO aiResponse = openAIService.askAssistant(chat.getPreviousResponseId(),userMessage.message(), lead);
        ResponseDTO response = handleAIAction(aiResponse,chat);
        storeMessages(chat,userMessage,aiResponse.getMensagem());

        return response;
    };

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

    private void createOrUpdateLead(AIResponseDTO response,ChatSessionEntity chat){
        LeadEntity databaseLead = chat.getLead();
        Lead assistantLead = response.getLead();

        // Card não existe no Pipefy e tenho o E-mail possibilitando assim a criação
        if(databaseLead == null && assistantLead.getEmail() != null){
            // Verifico se já existe um Lead com esse email, caso tenha trocado de Sessão
            databaseLead = leadRepository.findByEmailIgnoreCase(assistantLead.getEmail())
                    .orElseGet(() -> {
                        LeadEntity newLead = new LeadEntity(assistantLead.getEmail());
                        String cardId = pipefyService.createCardWithEmail(assistantLead.getEmail());
                        newLead.setCardId(cardId);
                        return leadRepository.save(newLead);
                    });
            chat.setLead(databaseLead);
            chatSessionRepository.save(chat);
        }
        // Lead existe e eu só atualizo ele
        if(databaseLead != null){
            databaseLead.setName(assistantLead.getNome());
            databaseLead.setEmail(assistantLead.getEmail());
            databaseLead.setCompany(assistantLead.getEmpresa());
            databaseLead.setNecessity(assistantLead.getNecessidade());
            databaseLead.setInterested(assistantLead.getInteresse());
            pipefyService.updateCardFields(databaseLead.getCardId(), assistantLead.getNome(), assistantLead.getEmail(), assistantLead.getEmpresa(), assistantLead.getNecessidade(), assistantLead.getInteresse());
            leadRepository.save(databaseLead);
        }
    }

    private void storeMessages(ChatSessionEntity chat, MessageDTO userMessage,String assistantMessage){
        messageRepository.save(new MessageEntity(Sender.USER, userMessage.message(),chat));
        messageRepository.save(new MessageEntity(Sender.ASSISTANT,assistantMessage,chat));
    }

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
