package com.yash.nerve.tools;

import com.yash.nerve.config.ChatContext;
import com.yash.nerve.models.Chat;
import com.yash.nerve.repository.ChatRepository;
import com.yash.nerve.service.MemoryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class MemoryTool {
    private MemoryService memoryService;
    private ChatRepository chatRepository;
    private ChatContext chatContext;
    public MemoryTool(MemoryService memoryService, ChatRepository chatRepository, ChatContext chatContext) {
        this.memoryService = memoryService;
        this.chatRepository = chatRepository;
        this.chatContext = chatContext;
    }
    @Tool(description = """
Update long-term memory with important user information such as:
- name
- preferences
- goals
- ongoing projects
- personal facts the user wants remembered
""")
    public void updateMemory() throws IOException {
        try{
            memoryService.updateMemory(chatContext.getId());
        }
        catch (Exception ex){
            ex.getMessage();
        }
    }
}
