package com.yash.nerve.controller;

import com.yash.nerve.models.AgentRequest;
import com.yash.nerve.service.LLMService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/chat")
@CrossOrigin(value = "*")
public class ChatController {
    private LLMService llmService;

    public ChatController(LLMService llmService) {
        this.llmService = llmService;
    }

    @PostMapping(value = "/ollama/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> simpleChat(@RequestBody AgentRequest request) throws IOException {
        Flux<String> result= llmService.chat(request);
        return  ResponseEntity.ok(result);
    }

}
