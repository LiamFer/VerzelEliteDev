package com.verzel.challenge.controller;

import com.verzel.challenge.dto.chat.MessageDTO;
import com.verzel.challenge.dto.chat.ResponseDTO;
import com.verzel.challenge.service.ChatService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController()
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<ResponseDTO> sendChatMessage(
            @RequestBody @Valid MessageDTO userMessage,
            @CookieValue(value = "sessionId", required = false) String sessionId,
            HttpServletResponse response
    ) {
        // Se não tem uma Session ID eu crio para o Usuário
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            int maxAge = 30 * 60;
            String cookieHeader = String.format(
                    "sessionId=%s; Max-Age=%d; Path=/; %s; SameSite=None; Partitioned",
                    sessionId,
                    maxAge,
                    "Secure"
            );
            response.setHeader("Set-Cookie", cookieHeader);
        }
        ResponseDTO chatResponse = chatService.handleMessage(userMessage, sessionId);
        return ResponseEntity.ok(chatResponse);
    }

}
