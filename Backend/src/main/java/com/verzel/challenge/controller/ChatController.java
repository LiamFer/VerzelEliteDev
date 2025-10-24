package com.verzel.challenge.controller;

import com.verzel.challenge.service.CalendlyService;
import com.verzel.challenge.service.PipefyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/chat")
public class ChatController {
    private final PipefyService pipefyService;
    private final CalendlyService calendlyService;

    public ChatController(PipefyService pipefyService, CalendlyService calendlyService) {
        this.pipefyService = pipefyService;
        this.calendlyService = calendlyService;
    }

    @GetMapping("/message")
    public ResponseEntity<?> sendChatMessage(){
        String ans = pipefyService.createCard("adolfo","adolfo23@email.com","grv","bolas",true,"linkzao do google");
        return ResponseEntity.ok(pipefyService.updateCardFields("1242194202",false,"fre fray"));
    }

    @GetMapping("/calendly")
    public ResponseEntity<?> testCalendly(){
        return ResponseEntity.ok(calendlyService.getAvailableSlots());
    }
}
