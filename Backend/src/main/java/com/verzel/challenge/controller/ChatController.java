package com.verzel.challenge.controller;

import com.verzel.challenge.dto.chat.AIResponseDTO;
import com.verzel.challenge.dto.chat.MessageDTO;
import com.verzel.challenge.service.CalendlyService;
import com.verzel.challenge.service.ChatService;
import com.verzel.challenge.service.OpenAIService;
import com.verzel.challenge.service.PipefyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController()
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<AIResponseDTO> sendChatMessage(@RequestBody @Valid MessageDTO userMessage,
                                                         @CookieValue(value = "sessionId", required = true) String sessionId) {
        return ResponseEntity.ok(chatService.handleMessage(userMessage, sessionId));
    }

//    @GetMapping("/message")
//    public ResponseEntity<?> sendChatMessage(){
//        return ResponseEntity.ok(pipefyService.getUserLeadByEmail("adolfo23@email.com"));
//    }

//    @GetMapping("/message")
//    public ResponseEntity<?> sendChatMessage(){
//        String ans = pipefyService.createCard("adolfo","adolfo23@email.com","grv","AJuda no Business",true,"linkzao do google");
//        return ResponseEntity.ok(pipefyService.updateCardFields("1242194202",false,"fre fray"));
//    }

//    @GetMapping("/calendly")
//    public ResponseEntity<?> testCalendly(){
//        return ResponseEntity.ok(calendlyService.getAvailableSlots());
//    }
}
