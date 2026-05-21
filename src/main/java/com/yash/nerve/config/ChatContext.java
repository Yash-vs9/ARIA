package com.yash.nerve.config;

import com.yash.nerve.models.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatContext {
    private Long chatId;
    private ChatContext(Long chatId){
        this.chatId=chatId;
    }
    public void setId(Long chatId){
        this.chatId=chatId;
    }
    public Long getId(){
        return this.chatId;
    }
    public ChatContext(){}
}
