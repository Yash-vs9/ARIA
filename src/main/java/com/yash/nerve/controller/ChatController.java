package com.yash.nerve.controller;

import com.yash.nerve.models.AgentRequest;
import com.yash.nerve.models.Chat;
import com.yash.nerve.models.ChatMessage;

import com.yash.nerve.models.Memory;
import com.yash.nerve.repository.ChatRepository;
import com.yash.nerve.service.LLMService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
@CrossOrigin(value = "*")
public class ChatController {


    private final ChatRepository chatRepository;
    private LLMService llmService;
    private Long activeChatId;

    public ChatController(ChatRepository chatRepository, LLMService llmService) {
        this.chatRepository = chatRepository;
        this.llmService = llmService;

    }

    @PostMapping(value = "/ollama/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> simpleChat(@RequestBody AgentRequest request) throws IOException {
        Flux<String> result= llmService.chat(request,activeChatId);
        return  ResponseEntity.ok(result);
    }
    @GetMapping("/findChats")
    public List<Chat> findAllChats(){
        return chatRepository.findAll();
    }
    @GetMapping("/messages")
    public List<ChatMessage> getMessages(@RequestParam Long id){
        Chat chat=chatRepository.findById(id).orElseThrow();
        this.activeChatId=id;
        List<ChatMessage> messages=chat.getMessages();
        return messages;
    }
    @GetMapping("/newChat")
    public Long newChat(){
        Chat chat=new Chat();
        chatRepository.save(chat);
        return chat.getId();
    }


}
