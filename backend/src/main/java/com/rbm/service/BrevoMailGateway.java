package com.rbm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoMailGateway implements MailGateway {
    private final RestClient http = RestClient.create();

    @Value("${app.mail.brevo.api-key:}")
    private String apiKey;

    @Value("${app.mail.from:no-reply@rbm.com}")
    private String from;

    @Override
    public void send(String to, String subject, String html) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY manquante");
        }
        http.post().uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "sender", Map.of("name", "Room & Event Booking", "email", from),
                        "to", List.of(Map.of("email", to)),
                        "subject", subject,
                        "htmlContent", html))
                .retrieve()
                .toBodilessEntity();
    }
}