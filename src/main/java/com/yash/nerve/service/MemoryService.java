package com.yash.nerve.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.nerve.models.Chat;
import com.yash.nerve.models.ChatMessage;
import com.yash.nerve.models.Memory;
import com.yash.nerve.repository.ChatRepository;
import com.yash.nerve.repository.MemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MemoryService {

    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;
    private final ConversationHistory conversationHistory;
    private final MemoryRepository memoryRepository;
    private final ChatRepository chatRepository;
    public MemoryService(ObjectMapper objectMapper,
                         ChatModel chatModel,
                         ConversationHistory conversationHistory,
                         MemoryRepository memoryRepository, ChatRepository chatRepository) {
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.conversationHistory = conversationHistory;
        this.memoryRepository = memoryRepository;
        this.chatRepository = chatRepository;
    }

    public Memory loadMemory() throws IOException {
        return memoryRepository.readFile();
    }

    public void updateMemory(Long id) throws IOException {
        Memory currentMemory = loadMemory();
        Chat chat=chatRepository.findById(id).orElseThrow();
        List<ChatMessage> messages1=chat.getMessages();

        String promptText = buildExtractionPrompt(currentMemory, messages1);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ChatResponse response = chatModel.call(new Prompt(promptText));
                String json = response.getResult().getOutput().getText().trim();
                System.out.println("🔍 Raw JSON to parse: " + json); // ← add this

                // Strip markdown code blocks if model wraps in ```json ... ```
                if (json.contains("```")) {
                    json = json.replaceAll("```json", "")
                            .replaceAll("```", "")
                            .trim();
                }

                // Extract just the JSON object even if model adds text around it
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start != -1 && end != -1) {
                    json = json.substring(start, end + 1);
                } else {
                    throw new RuntimeException("No JSON object found in response: " + json);
                }

                Memory updatedMemory = objectMapper.readValue(json, Memory.class);
                memoryRepository.writeFile(updatedMemory);
                System.out.println("✅ Memory updated successfully on attempt " + attempt);
                return;

            } catch (Exception e) {
                System.err.println("⚠ Memory update attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    System.err.println("All retries failed — keeping existing memory unchanged");
                    // Don't crash the app — silently keep old memory
                }
            }
        }
    }

    private String buildExtractionPrompt(Memory currentMemory, List<ChatMessage> messages) {
        return """
                You are a JSON extraction agent.
                
                YOUR ONLY JOB: Return a single JSON object. Nothing else.
                
                STRICT RULES:
                - NO markdown, NO code blocks, NO backticks
                - NO explanation before or after the JSON
                - NO "Here is..." or "Based on..." or any other text
                - ONLY the raw JSON object starting with { and ending with }
                - Use EXACTLY these field names: username, preferences, history
                - preferences: list of user preferences as strings
                - history: list of important facts about the user as strings, keep it short and factual
                - Keep existing values if no new information is found
                - Add new facts, do not remove existing ones
                
                Existing memory: %s
                
                Conversation to extract from: %s
                
                Output ONLY valid JSON in this exact structure, nothing else:
                {"username":"","preferences":[],"history":[]}
                """.formatted(currentMemory, formatMessages(messages));
    }

    private String formatMessages(List<ChatMessage> messages) {

        StringBuilder sb = new StringBuilder();

        for (ChatMessage message : messages) {

            sb.append(message.getRole())
                    .append(": ")
                    .append(message.getMessage())
                    .append("\n");
        }

        return sb.isEmpty()
                ? "No conversation yet."
                : sb.toString();
    }
}