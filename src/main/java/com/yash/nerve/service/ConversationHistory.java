package com.yash.nerve.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;

@Component
public class ConversationHistory {
    private List<Message> history=new ArrayList<>();
    public static final int MAX_LEN=4;

    public void addUserMessage(String content){
        history.add(new UserMessage(content));
    }
    public void addAssistantMessage(String content){
        history.add(new AssistantMessage(content));
    }
    public void clear(){
        this.history.clear();
    }
    public List<Message> getLastN(){
        int size=history.size();
        if(size<MAX_LEN){
            return history;
        }
        return history.subList(size-MAX_LEN,size);
    }
}
