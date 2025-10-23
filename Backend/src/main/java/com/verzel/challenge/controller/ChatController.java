package com.verzel.challenge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/chat")
public class ChatController {

    @PostMapping("/message")
    public ResponseEntity<String> sendChatMessage(){
        return ResponseEntity.ok("Message");
    }
}
