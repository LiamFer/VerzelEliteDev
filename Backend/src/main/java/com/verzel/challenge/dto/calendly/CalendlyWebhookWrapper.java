package com.verzel.challenge.dto.calendly;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendlyWebhookWrapper {
    private String event;
    private String created_at;
    private WebhookPayload payload;
}
