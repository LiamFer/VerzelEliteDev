package com.verzel.challenge.service;

import com.verzel.challenge.dto.chat.AIResponseBodyDTO;
import com.verzel.challenge.dto.chat.AIResponseDTO;
import com.verzel.challenge.dto.chat.MessageDTO;
import com.verzel.challenge.dto.pipefy.Card;
import com.verzel.challenge.dto.pipefy.Lead;
import com.verzel.challenge.entity.ChatSessionEntity;
import com.verzel.challenge.entity.LeadEntity;
import com.verzel.challenge.entity.MessageEntity;
import com.verzel.challenge.mapper.LeadMapper;
import com.verzel.challenge.repository.ChatSessionRepository;
import com.verzel.challenge.repository.LeadRepository;
import com.verzel.challenge.repository.MessageRepository;
import com.verzel.challenge.type.Sender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {
    private final OpenAIService openAIService;
    private final PipefyService pipefyService;
    private final CalendlyService calendlyService;
    private final ChatSessionRepository chatSessionRepository;
    private final LeadRepository leadRepository;
    private final MessageRepository messageRepository;

    public ChatService(OpenAIService openAIService, PipefyService pipefyService, CalendlyService calendlyService, ChatSessionRepository chatSessionRepository, LeadRepository leadRepository, MessageRepository messageRepository) {
        this.openAIService = openAIService;
        this.pipefyService = pipefyService;
        this.calendlyService = calendlyService;
        this.chatSessionRepository = chatSessionRepository;
        this.leadRepository = leadRepository;
        this.messageRepository = messageRepository;
    }

    public AIResponseDTO handleMessage(MessageDTO userMessage,String sessionId){
        ChatSessionEntity chat = getChatBySessionId(sessionId);
        Lead lead = LeadMapper.toLead(chat.getLead());
        AIResponseDTO aiResponse = openAIService.askAssistant(chat.getPreviousResponseId(),userMessage.message(), lead);
        handleCollectedData(aiResponse,chat);
        System.out.println("A mensagem da IA ai olha: " + aiResponse.getMensagem());
        storeMessages(chat,userMessage,aiResponse.getMensagem());
        return aiResponse;
    };

    private void handleCollectedData(AIResponseDTO response,ChatSessionEntity chat){
        LeadEntity databaseLead = chat.getLead();
        Lead assistantLead = response.getLead();
        chat.setPreviousResponseId(response.getId());

        // Card não existe no Pipefy e tenho o E-mail possibilitando assim a criação
        if(databaseLead == null && assistantLead.getEmail() != null){
            LeadEntity newLead = new LeadEntity(assistantLead.getEmail());
            String cardId = pipefyService.createCardWithEmail(assistantLead.getEmail());
            newLead.setCardId(cardId);
            leadRepository.save(newLead);

            chat.setLead(newLead);
            chatSessionRepository.save(chat);

            databaseLead = newLead;
        }

        // Atualizar o Card durante a Conversa
        if(databaseLead != null){
            databaseLead.setName(assistantLead.getNome());
            databaseLead.setEmail(assistantLead.getEmail());
            databaseLead.setCompany(assistantLead.getEmpresa());
            databaseLead.setNecessity(assistantLead.getNecessidade());
            databaseLead.setInterested(assistantLead.getInteresse());

            pipefyService.updateCardFields(databaseLead.getCardId(), assistantLead.getNome(), assistantLead.getEmail(), assistantLead.getEmpresa(), assistantLead.getNecessidade(), assistantLead.getInteresse(),"");
            leadRepository.save(databaseLead);
        }
    }

    private void storeMessages(ChatSessionEntity chat, MessageDTO userMessage,String assistantMessage){
        messageRepository.save(new MessageEntity(Sender.USER, String.valueOf(userMessage.message()),chat));
        messageRepository.save(new MessageEntity(Sender.ASSISTANT, String.valueOf(assistantMessage),chat));
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
