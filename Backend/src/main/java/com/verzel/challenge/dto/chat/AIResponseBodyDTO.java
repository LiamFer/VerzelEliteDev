package com.verzel.challenge.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verzel.challenge.dto.pipefy.Lead;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIResponseBodyDTO {

    private String id;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    private List<Output> output;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output {
        private String id;
        private String type;
        private String status;
        private List<Content> content;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private String type;

        @JsonProperty("text")
        private String text;

        public AssistantText getAssistantText(ObjectMapper mapper) {
            if (text == null) return null;
            try {
                return mapper.readValue(text, AssistantText.class);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao desserializar AssistantText", e);
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssistantText {
        private String mensagem;
        private Lead lead;
    }

    public AIResponseDTO getResponse() {
        ObjectMapper mapper = new ObjectMapper();
        return new AIResponseDTO(id, getAssistantMessage(mapper), getAssistantLead(mapper));
    }

    public String getAssistantMessage(ObjectMapper mapper) {
        if (output == null || output.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Output o : output) {
            if (o.getContent() != null) {
                for (Content c : o.getContent()) {
                    if ("output_text".equals(c.getType())) {
                        AssistantText at = c.getAssistantText(mapper);
                        if (at != null && at.getMensagem() != null) {
                            sb.append(at.getMensagem());
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public Lead getAssistantLead(ObjectMapper mapper) {
        if (output == null || output.isEmpty()) return null;
        for (Output o : output) {
            if (o.getContent() != null) {
                for (Content c : o.getContent()) {
                    if ("output_text".equals(c.getType())) {
                        AssistantText at = c.getAssistantText(mapper);
                        if (at != null && at.getLead() != null) {
                            return at.getLead();
                        }
                    }
                }
            }
        }
        return null;
    }
}