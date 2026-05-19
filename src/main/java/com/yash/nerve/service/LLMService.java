package com.yash.nerve.service;

import com.yash.nerve.models.AgentRequest;
import com.yash.nerve.models.Memory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LLMService {
    private final MemoryService memoryService;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ConversationHistory conversationHistory;
    public LLMService(MemoryService memoryService, ChatModel chatModel, StreamingChatModel streamingChatModel, ConversationHistory conversationHistory) {
        this.memoryService = memoryService;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.conversationHistory = conversationHistory;
    }

    public Flux<String> chat(AgentRequest request) throws IOException {
        if(request.prompt().equals("bye")){
            memoryService.updateMemory();
            return Flux.just("bye");
        }
        conversationHistory.addUserMessage(request.prompt());
        Memory longMemory=memoryService.loadMemory();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("""
        You are NERVE, a local AI assistant.
        
        RULES YOU MUST FOLLOW:
        - Answer only what the user asks. Nothing more.
        - Never make up information you don't know.
        - If you don't know something, say "I don't know."
        - Keep responses short and direct.
        - Never roleplay or pretend to be something else.
        - Do not add unsolicited suggestions or commentary.
        
        WHAT YOU KNOW ABOUT THE USER:
        - Name: %s
        - Preferences: %s
        """.formatted(
                longMemory.getUsername() != null ? longMemory.getUsername() : "unknown",
                longMemory.getPreferences() != null ? longMemory.getPreferences() : "none"
        )));
        messages.addAll(conversationHistory.getLastN());
        messages.add(new UserMessage(request.prompt()));

        StringBuilder fullResponse = new StringBuilder();  // accumulates tokens

        return streamingChatModel.stream(new Prompt(messages))
                .map(response -> response.getResult().getOutput().getText())
                .doOnNext(token -> fullResponse.append(token))           // collect silently
                .doOnComplete(() ->                                       // save when done
                        conversationHistory.addAssistantMessage(fullResponse.toString())
                );

    }

}
