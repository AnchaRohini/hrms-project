package com.mentis.hrms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        You are Menti, a helpful AI assistant for Menti's IT Solutions HRMS system.
        You are friendly, concise, and professional.

        You help employees with:
        - Leave balance enquiries and leave applications (Sick: 12 days, Casual: 7 days, Earned: 18 days)
        - Attendance tracking and history
        - Document submission and verification status
        - HR policies and company guidelines
        - Payroll information (employees can check their salary details in Profile > Work Details)
        - HRMS navigation guidance

        Key policies:
        - Leave requires minimum 2 days advance notice
        - Working hours: 9 AM - 6 PM
        - Attendance is tracked via check-in/check-out on the dashboard
        - Documents must be submitted before the deadline shown in Profile > Documents

        Keep responses concise (2-4 sentences). If you cannot find specific data about the
        employee, guide them to the correct section of the dashboard. Always be empathetic.
        """;

    public String chat(String userMessage, List<MessagePair> conversationHistory, String employeeContext) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            ArrayNode messages = requestBody.putArray("messages");

            // System message with employee context
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT + "\n\nEmployee context: " + employeeContext);

            // Add conversation history
            for (MessagePair pair : conversationHistory) {
                ObjectNode userNode = messages.addObject();
                userNode.put("role", "user");
                userNode.put("content", pair.getUserMessage());

                ObjectNode assistantNode = messages.addObject();
                assistantNode.put("role", "assistant");
                assistantNode.put("content", pair.getAssistantMessage());
            }

            // Current user message
            ObjectNode currentMsg = messages.addObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", userMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.debug("Sending request to Groq API with model: {}", model);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String reply = responseJson
                        .path("choices").get(0)
                        .path("message")
                        .path("content")
                        .asText("I'm having trouble responding right now. Please try again.");
                log.debug("Groq API responded successfully");
                return reply;
            }

            return "I'm having trouble connecting. Please try again in a moment.";

        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage(), e);
            return "I'm experiencing a technical issue. Please try again shortly.";
        }
    }

    // Inner class for conversation history pairs
    public static class MessagePair {
        private final String userMessage;
        private final String assistantMessage;

        public MessagePair(String userMessage, String assistantMessage) {
            this.userMessage = userMessage;
            this.assistantMessage = assistantMessage;
        }

        public String getUserMessage() { return userMessage; }
        public String getAssistantMessage() { return assistantMessage; }
    }
}