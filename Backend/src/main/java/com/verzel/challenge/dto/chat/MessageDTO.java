package com.verzel.challenge.dto.chat;
import jakarta.validation.constraints.NotBlank;

public record MessageDTO (@NotBlank(message = "A mensagem não pode estar vazia")
                String message) {
}
