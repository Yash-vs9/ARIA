package com.yash.nerve.service;

import com.yash.nerve.config.ChatContext;
import com.yash.nerve.models.AgentRequest;

import com.yash.nerve.models.Chat;
import com.yash.nerve.models.ChatMessage;
import com.yash.nerve.models.Memory;
import com.yash.nerve.repository.ChatRepository;
import com.yash.nerve.tools.FileTools;
import com.yash.nerve.tools.MemoryTool;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class LLMService {

    private final MemoryService memoryService;
    private final ChatModel chatModel;
    private final ConversationHistory conversationHistory;
    private final FileTools fileTools;
    private final ShellTools shellTools;
    private final ChatContext chatContext;
    private final GoogleCalendarService googleCalendarService;
    private final SystemTools systemTools;
    private final GmailService gmailService;
    private final MemoryTool memoryTool;
    private final ChatRepository chatRepository;
    public LLMService(MemoryService memoryService,
                      ChatModel chatModel,
                      ConversationHistory conversationHistory,
                      FileTools fileTools,
                      ShellTools shellTools, ChatContext chatContext, GoogleCalendarService googleCalendarService,
                      SystemTools systemTools, GmailService gmailService, MemoryTool memoryTool, ChatRepository chatRepository) {
        this.memoryService = memoryService;
        this.chatModel = chatModel;
        this.conversationHistory = conversationHistory;
        this.fileTools = fileTools;
        this.shellTools = shellTools;
        this.chatContext = chatContext;
        this.googleCalendarService = googleCalendarService;
        this.systemTools = systemTools;
        this.gmailService = gmailService;
        this.memoryTool = memoryTool;
        this.chatRepository = chatRepository;
    }

    public Flux<String> chat(AgentRequest request, Long chatId) throws IOException {

        if (request.prompt().equalsIgnoreCase("bye")) {
            memoryService.updateMemory(chatContext.getId());
            return Flux.just("Goodbye! Memory updated. See you next time.");
        }

        Chat chat = chatRepository.findById(chatId).orElseThrow();
        chatContext.setId(chatId);
        // Save user message immediately
        ChatMessage userMessage = new ChatMessage();
        userMessage.setMessage(request.prompt());
        userMessage.setRole("USER");
        userMessage.setTimestamp(LocalDateTime.now());
        userMessage.setChat(chat);

        chat.getMessages().add(userMessage);
        chatRepository.save(chat);

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

        // Load chat history from database
        for (ChatMessage msg : chat.getMessages()) {

            if ("USER".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getMessage()));
            } else {
                messages.add(
                        new org.springframework.ai.chat.messages.AssistantMessage(
                                msg.getMessage()
                        )
                );
            }
        }

        ChatResponse response = chatModel.call(
                new Prompt(
                        messages,
                        ToolCallingChatOptions.builder()
                                .toolCallbacks(
                                        ToolCallbacks.from(
                                                fileTools,
                                                shellTools,
                                                systemTools,
                                                gmailService,
                                                memoryTool,
                                                googleCalendarService

                                        )
                                )
                                .build()
                )
        );

        String finalAnswer = response.getResult().getOutput().getText();

        return Flux.fromArray(finalAnswer.split("(?<=\\s)|(?=\\s)"))
                .delayElements(Duration.ofMillis(15))
                .doOnComplete(() -> {

                    ChatMessage assistantMessage = new ChatMessage();
                    assistantMessage.setMessage(finalAnswer);
                    assistantMessage.setRole("ASSISTANT");
                    assistantMessage.setTimestamp(LocalDateTime.now());
                    assistantMessage.setChat(chat);

                    chat.getMessages().add(assistantMessage);

                    chatRepository.save(chat);
                });
    }
}