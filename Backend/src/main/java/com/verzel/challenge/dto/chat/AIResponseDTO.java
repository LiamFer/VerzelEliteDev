package com.verzel.challenge.dto.chat;

import com.verzel.challenge.dto.pipefy.Lead;
import com.verzel.challenge.type.ActionAI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseDTO {
    private String id;
    private String mensagem;
    private Lead lead;
    private ActionAI action;
}