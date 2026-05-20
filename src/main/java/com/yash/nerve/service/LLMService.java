package com.yash.nerve.service;

import com.yash.nerve.models.AgentRequest;
import com.yash.nerve.models.Memory;
import com.yash.nerve.tools.FileTools;
import com.yash.nerve.tools.ShellTools;
import com.yash.nerve.tools.SystemTools;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class LLMService {

    private final MemoryService memoryService;
    private final ChatModel chatModel;
    private final ConversationHistory conversationHistory;
    private final FileTools fileTools;
    private final ShellTools shellTools;
    private final SystemTools systemTools;

    public LLMService(MemoryService memoryService,
                      ChatModel chatModel,
                      ConversationHistory conversationHistory,
                      FileTools fileTools,
                      ShellTools shellTools,
                      SystemTools systemTools) {
        this.memoryService = memoryService;
        this.chatModel = chatModel;
        this.conversationHistory = conversationHistory;
        this.fileTools = fileTools;
        this.shellTools = shellTools;
        this.systemTools = systemTools;
    }

    public Flux<String> chat(AgentRequest request) throws IOException {

        if (request.prompt().equalsIgnoreCase("bye")) {
            memoryService.updateMemory();
            conversationHistory.clear();
            return Flux.just("Goodbye! Memory updated. See you next time.");
        }

        conversationHistory.addUserMessage(request.prompt());
        Memory memory = memoryService.loadMemory();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
                You are NERVE, a smart local AI assistant.
                
                RULES:
                - Answer conversational messages naturally
                - For system info (RAM, disk, CPU, files) — always use the appropriate tool
                - Never guess system information — call the tool
                - Present tool results clearly and concisely to the user
                - Never output raw JSON or function call syntax
                
                USER: name=%s, preferences=%s
                """.formatted(
                memory.getUsername() != null ? memory.getUsername() : "unknown",
                memory.getPreferences() != null ? memory.getPreferences() : "none"
        )));

        messages.addAll(conversationHistory.getLastN());
        messages.add(new UserMessage(request.prompt()));

        // Pass all tools — Gemini handles tool calling reliably
        ChatResponse response = chatModel.call(
                new Prompt(messages, ToolCallingChatOptions.builder()
                        .toolCallbacks(ToolCallbacks.from(fileTools, shellTools, systemTools))
                        .build())
        );

        String finalAnswer = response.getResult().getOutput().getText();

        return Flux.fromArray(finalAnswer.split("(?<=\\s)|(?=\\s)"))
                .delayElements(Duration.ofMillis(15))
                .doOnComplete(() ->
                        conversationHistory.addAssistantMessage(finalAnswer)
                );
    }
}