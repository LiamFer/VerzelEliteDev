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
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("sessionId", sessionId);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(30 * 60);
            response.addCookie(cookie);
        }
        ResponseDTO chatResponse = chatService.handleMessage(userMessage, sessionId);
        return ResponseEntity.ok(chatResponse);
    }

}
