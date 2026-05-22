# ARIA

> An AI agent platform built with Spring Boot, Spring AI, Gemini, and React that transforms natural language into executable workflows through memory, tool calling, and deterministic orchestration.

---

# Overview

ARIA is a production-oriented AI assistant designed to transform natural language requests into real-world actions.

Rather than acting as a traditional chatbot, ARIA focuses on task execution through deterministic orchestration, tool calling, memory management, and structured AI interactions.

The project combines Spring Boot, Spring AI, Gemini, and a React frontend to build a practical AI agent capable of reasoning, using tools, maintaining context, and executing multi-step workflows.

The primary engineering challenge is making AI systems reliable through strong software architecture rather than relying solely on model capability.

---

# Why ARIA?

Most AI applications stop at generating text.

NERVE focuses on building a complete AI system capable of:

- Understanding user intent
- Managing conversational context
- Persisting memory
- Calling tools
- Executing actions
- Orchestrating multi-step workflows

The project is designed to explore how modern AI agents can be built using strong backend engineering practices, deterministic logic, and reliable software architecture.

---

# Architecture

```text
┌──────────────────────┐
│      React PWA       │
│   User Interface     │
└──────────┬───────────┘
           │ HTTP / SSE
           ▼
┌──────────────────────┐
│     Spring Boot      │
│    Agent Runtime     │
└──────────┬───────────┘
           │
           ├── Conversation History
           ├── Memory System
           ├── Prompt Construction
           ├── Tool Execution
           ├── Task Planning
           └── Response Streaming
           │
           ▼
┌──────────────────────┐
│      Spring AI       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│       Gemini         │
│    Google AI Model   │
└──────────────────────┘
```

---

# Tech Stack

## Frontend

- React
- Progressive Web App (PWA)
- Server-Sent Events (SSE)

## Backend

- Java 21
- Spring Boot 3.5.14
- Spring AI
- Maven

## AI

- Google Gemini
- Tool Calling
- Structured Prompting
- Context Engineering

## Storage

- JSON-based Memory Store
- In-Memory Conversation Context

---

# Core Features

## Conversational AI

- Standard chat responses
- Streaming responses
- Context-aware conversations
- System-level instruction control

---

## Memory System

Hybrid memory architecture designed to balance performance and context quality.

### Short-Term Memory

Conversation window containing recent messages.

Features:

- Session continuity
- Sliding message window
- Fast retrieval
- Low token usage

### Long-Term Memory

Persistent structured memory stored separately from chat history.

Examples:

- User preferences
- Operating system
- Project details
- Frequently referenced information

This memory is injected into prompts as structured facts rather than raw transcripts.

---

## Tool Execution

ARIA can perform actions through backend tools rather than only generating text.

Current and planned tool categories:

- File operations
- Shell commands
- Web search
- Content extraction
- Email automation
- Calendar management
- Browser automation

All tool execution occurs within Spring Boot.

The frontend never interacts directly with the operating system.

It can answer questions like -> Why is my os is running slow? (The LLM checks RAM usage , Disk space left , Processes then gives accurate answer)

---

## Streaming Responses

Responses are streamed using Spring WebFlux and Server-Sent Events.

```java
Flux<String>
```

Benefits:

- Real-time feedback
- Lower perceived latency
- Better conversational experience

---

# Project Structure

```text
src/main/java/com/yash/nerve/

├── controller/
│   └── ChatController.java
│
├── service/
│   ├── LLMService.java
│   ├── ConversationHistory.java
│   ├── MemoryService.java
│   ├── GmailService.java
│   └── AgentService.java
│   └── GoogleCalenderService.java
│
├── repository/
│   └── MemoryRepository.java
│
├── models/
│   └── AgentRequest.java
│   └── Chat.java
│   └── ChatMessage.java
│   └── Memory.java
│
├── tools/
│   ├── FileTools.java
│   ├── ShellTools.java
│   ├── CalendarTools.java
│   └── Memory.java
│
├── config/
│   └── ChatContext.java
│   └── PathValidator.java
│   └── SandboxConfig.java
│
└── NerveApplication.java
```

---

# Memory Architecture

NERVE intentionally avoids storing entire conversations indefinitely.

Instead, it uses a hybrid memory system.

## Long-Term Memory

Stored as structured facts.

Example:

```json
{
  "username" : "your_name",
  "preferences" : [ ],
  "history" : [  ]
}
```

These facts are injected into the system prompt during every interaction.

---

## Short-Term Memory

Recent conversational context.

Example:

```text
User Message
Assistant Response
User Message
Assistant Response
...
```

Provides conversational continuity while maintaining efficiency.

---

# Engineering Decisions

## Constructor Injection

Used throughout the codebase.

```java
public LLMService(ChatModel chatModel) {
    this.chatModel = chatModel;
}
```

Benefits:

- Explicit dependencies
- Easier testing
- Better immutability
- Improved maintainability

---

## Records for DTOs

```java
public record AgentRequest(String prompt) {}
```

Benefits:

- Less boilerplate
- Immutable data structures
- Cleaner APIs

---

## Deterministic Orchestration

Rather than relying entirely on AI reasoning:

```text
Java Logic
     ↓
Context Gathering
     ↓
Memory Injection
     ↓
Tool Selection
     ↓
AI Reasoning
     ↓
Response
```

Critical application logic remains deterministic and testable.

---

## Model Agnostic Design

ARIA uses Spring AI abstraction.

Benefits:

- Easy model replacement
- Vendor independence
- Cleaner architecture

Potential future integrations:

- Ollama-hosted models
- Local LLMs


---


# Current Progress

## Completed

- Spring Boot setup
- Spring AI integration
- Gemini integration
- Chat endpoint
- Streaming endpoint
- Controller-service architecture
- DTO design using records
- System prompt support
- Basic tool infrastructure
- File tool implementation



---

# Running Locally

## Clone Repository

```bash
git clone https://github.com/yourusername/ARIA.git
cd ARIA
```

---

## Configure application.yaml


```yaml
spring:
  datasource:
    url: jdbc:sqlite:aria.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update
    show-sql: true

  application:
    name: aria

  ai:
    google:
      genai:
        chat:
          options:
            model: gemini-3.1-flash-lite
        api-key: ${GEMINI_API_KEY}
    model:
      chat: google-genai


server:
  port: 8080

# Custom nerve config
nerve:
  memory:
    path: ./memory.json
  sandbox:
    path: /Users/yash/nerve-sandbox/workspace
serpapi:
  api-key:${SERP_API_KEY}
```

or configure Gemini according to the Spring AI version being used.

---

## Start Backend

```bash
./mvnw spring-boot:run
```

---

## Start Frontend

```bash
npm install
npm run dev
```

Frontend:

```text
http://localhost:3000
```

Backend:

```text
http://localhost:8080
```

---

# Learning Goals

ARIA is also a vehicle for mastering:

- Java 21
- Spring Boot Internals
- Dependency Injection
- Bean Lifecycle
- REST APIs
- Reactive Programming
- Spring AI
- AI Agent Architecture
- Memory Systems
- Tool Calling
- Context Engineering
- System Design
- Production Backend Development

The objective is not only to build an AI assistant but to understand the engineering principles behind reliable AI systems.

---

# Roadmap

## Phase 1

- Chat
- Streaming
- Memory Foundation

## Phase 2

- File Operations
- Shell Commands
- Tool Framework

## Phase 3

- Google Search
- Content Extraction
- Research Workflows

## Phase 4

- Gmail Integration
- Calendar Automation
- Multi-Tool Coordination

## Phase 5

- Browser Automation
- Workflow Engine
- Advanced Planning

## Phase 6

- Long-Term Memory Improvements
- Context Optimization
- Production Deployment

---

# Design Principles

- Local-first development
- Strong backend architecture
- Deterministic execution where possible
- AI-assisted reasoning where beneficial
- Maintainable code over clever code
- Reliability over model size
- Production-quality engineering practices

---

# License

MIT License

---

Built with Java, Spring Boot, Spring AI, Gemini, and curiosity.
