package com.eventsphere.notification.service;

import com.eventsphere.notification.entity.SentNotification;
import com.eventsphere.notification.repository.SentNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final SentNotificationRepository notificationRepo;
    private final ObjectMapper objectMapper;

    @Value("${notification.sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${notification.from-email}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendTemplatedEmail(UUID userId, String toEmail, String subject,
                                   String templateName, Map<String, Object> variables) {
        Context ctx = new Context();
        variables.forEach(ctx::setVariable);
        String html = templateEngine.process(templateName, ctx);

        boolean stubMode = sendGridApiKey == null || sendGridApiKey.isBlank()
                || sendGridApiKey.startsWith("SG.placeholder");

        String status = "SENT";
        String errorMsg = null;

        if (stubMode) {
            log.info("📧 [STUB EMAIL] To: {} | Subject: {} | Template: {}", toEmail, subject, templateName);
        } else {
            try {
                sendViaSendGrid(toEmail, subject, html);
            } catch (Exception e) {
                log.error("Failed to send email via SendGrid to {}", toEmail, e);
                status = "FAILED";
                errorMsg = e.getMessage();
            }
        }

        notificationRepo.save(SentNotification.builder()
                .userId(userId).template(templateName).channel("email")
                .recipient(toEmail).subject(subject)
                .status(status).errorMessage(errorMsg)
                .build());
    }

    private void sendViaSendGrid(String to, String subject, String html) throws Exception {
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", to)))),
                "from", Map.of("email", fromEmail),
                "subject", subject,
                "content", List.of(Map.of("type", "text/html", "value", html))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + sendGridApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("SendGrid returned " + response.statusCode() + ": " + response.body());
        }
    }
}
