package com.verzel.challenge.controller;

import com.verzel.challenge.dto.calendly.CalendlyWebhookWrapper;
import com.verzel.challenge.dto.calendly.WebhookPayload;
import com.verzel.challenge.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/calendly")
public class CalendlyController {
    private final ChatService chatService;

    public CalendlyController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody CalendlyWebhookWrapper payload) {
        try {
            chatService.scheduleMeeting(payload.getPayload());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}