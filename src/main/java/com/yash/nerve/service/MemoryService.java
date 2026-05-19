package com.yash.nerve.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.nerve.models.Memory;
import com.yash.nerve.repository.MemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class MemoryService {
    private final ObjectMapper objectMapper=new ObjectMapper();
    private final ChatModel chatModel;
    private final ConversationHistory conversationHistory;
    private final MemoryRepository memoryRepository;
    public MemoryService(ChatModel chatModel, ConversationHistory conversationHistory, MemoryRepository memoryRepository) {
        this.chatModel = chatModel;
        this.conversationHistory = conversationHistory;
        this.memoryRepository = memoryRepository;
    }

    public Memory loadMemory() throws IOException {
        File file=new File("memory.json");
        return objectMapper.readValue(file,Memory.class);
    }
    public void updateMemory() throws IOException {
        Memory currentMemory=loadMemory();
        List<Message> messages=conversationHistory.getLastN();
        String promptText = """
            You are a memory agent. Extract important facts from this conversation.
            
            Current Memory: %s
            
            Last Conversation: %s
            
            Return updated memory as JSON only matching this structure exactly:
            {
              "username": "",
              "preferences": [],
              "history": []
            }
            IMPORTANT - make sure to return only json part, nothing more like this
            {
              "username": "",
              "preferences": [],
              "history": []
            }
            """.formatted(currentMemory, messages);

        ChatResponse response = chatModel.call(new Prompt(promptText));
        String json= response.getResult().getOutput().getText();
        Memory updatedMemory=objectMapper.readValue(json,Memory.class);
        memoryRepository.writeFile(updatedMemory);
        return;


    }
}
