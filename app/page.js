"use client"
import React, { useState, useRef, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';

import {
  Send,
  Cpu,
  User,
  Zap,
  Terminal,
  CheckCircle2,
  Database,
  Loader2
} from 'lucide-react';

export default function App() {
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'ai',
      content: 'System initialized. NERVE (Llama 3.2 3B) is online and connected to local environment. How can I assist you today?',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [tasks, setTasks] = useState([
    { id: 1, text: 'Loaded memory.json', status: 'done', icon: <Database size={14} /> },
    { id: 2, text: 'ConversationHistory initialized', status: 'done', icon: <CheckCircle2 size={14} /> },
    { id: 3, text: 'Ollama model loaded', status: 'done', icon: <Cpu size={14} /> },
  ]);

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'auto', block: 'end' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    if (!inputValue.trim() || isStreaming) return;

    const userMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    const aiMessageId = `ai-${Date.now()}`;

    setMessages(prev => [
      ...prev,
      userMessage,
      {
        id: aiMessageId,
        role: 'ai',
        content: '',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }
    ]);

    setInputValue('');
    setIsStreaming(true);

    try {
      const response = await fetch("http://localhost:8080/chat/ollama/stream", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "text/event-stream"
        },
        body: JSON.stringify({ prompt: userMessage.content })
      });

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

      const reader = response.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        let tokensInChunk = '';

        while (buffer.includes('\n\n')) {
          const eventEndIndex = buffer.indexOf('\n\n');
          const eventBlock = buffer.slice(0, eventEndIndex);
          buffer = buffer.slice(eventEndIndex + 2);

          const lines = eventBlock.split('\n');
          for (const line of lines) {
            if (line.startsWith('data:')) {
              let token = line.slice(5);
              token = token.replace(/\\n/g, '\n');
              if (line === 'data:') token = '\n';
              tokensInChunk += token;
            }
          }
        }

        if (tokensInChunk !== '') {
          setMessages(prev =>
            prev.map(msg =>
              msg.id === aiMessageId
                ? { ...msg, content: msg.content + tokensInChunk }
                : msg
            )
          );
        }
      }

      setTasks(prev => [{
        id: Date.now(),
        text: 'Stream complete',
        status: 'done',
        icon: <CheckCircle2 size={14} />
      }, ...prev]);

    } catch (error) {
      console.error("Error:", error);
      setMessages(prev => prev.map(msg =>
        msg.id === aiMessageId
          ? { ...msg, content: `Error: Unable to reach NERVE backend. Is Spring Boot running on port 8080? Details: ${error.message}` }
          : msg
      ));
    } finally {
      setIsStreaming(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="header">
        <div className="header-brand">
          <div className="header-logo">
            <Zap size={18} />
          </div>
          <div className="header-text">
            <h1 className="header-title">NERVE</h1>
            <p className="header-subtitle">Local Orchestrator</p>
          </div>
        </div>
        <div className="header-status">
          <span className="status-dot"></span>
          <span className="status-text">Llama 3.2 3B</span>
        </div>
      </header>

      {/* Main Container */}
      <div className="main-container">
        {/* Chat Panel */}
        <main className="chat-panel">
          <div className="messages-container">
            {messages.map((msg) => (
              <div key={msg.id} className={`message-row ${msg.role === 'user' ? 'message-row-user' : 'message-row-ai'}`}>
                <div className={`message-wrapper ${msg.role === 'user' ? 'message-wrapper-user' : 'message-wrapper-ai'}`}>
                  
                  {/* Avatar */}
                  <div className="avatar-container">
                    {msg.role === 'user' ? (
                      <div className="avatar avatar-user">
                        <User size={15} />
                      </div>
                    ) : (
                      <div className="avatar avatar-ai">
                        <Cpu size={15} />
                      </div>
                    )}
                  </div>

                  {/* Message Bubble */}
                  <div className={`message-content ${msg.role === 'user' ? 'message-content-user' : 'message-content-ai'}`}>
                    <div className={`message-bubble ${msg.role === 'user' ? 'bubble-user' : 'bubble-ai'}`}>
                      {msg.role === 'user' ? (
                        msg.content
                      ) : (
                        <div className="prose-content">
                          <ReactMarkdown
                            components={{
                              ul: ({node, ...props}) => <ul className="prose-list" {...props} />,
                              ol: ({node, ...props}) => <ol className="prose-list-ordered" {...props} />,
                              li: ({node, ...props}) => <li className="prose-list-item" {...props} />,
                              code: ({node, inline, className, children, ...props}) => {
                                const match = /language-(\w+)/.exec(className || '');
                                const isBlockCode = match || String(children).includes('\n');

                                return isBlockCode ? (
                                  <div className="code-block">
                                    <div className="code-header">
                                      <span>{match ? match[1] : 'code'}</span>
                                    </div>
                                    <SyntaxHighlighter
                                      style={oneDark}
                                      language={match ? match[1] : 'text'}
                                      PreTag="div"
                                      customStyle={{
                                        margin: 0,
                                        padding: '1rem',
                                        background: 'transparent',
                                        fontSize: '13px',
                                        lineHeight: '1.6'
                                      }}
                                      {...props}
                                    >
                                      {String(children).replace(/\n$/, '')}
                                    </SyntaxHighlighter>
                                  </div>
                                ) : (
                                  <code className="inline-code" {...props}>
                                    {children}
                                  </code>
                                );
                              },
                              strong: ({node, ...props}) => <strong className="prose-strong" {...props} />,
                              h1: ({node, ...props}) => <h1 className="prose-h1" {...props} />,
                              h2: ({node, ...props}) => <h2 className="prose-h2" {...props} />,
                              h3: ({node, ...props}) => <h3 className="prose-h3" {...props} />,
                              p: ({node, ...props}) => <p className="prose-p" {...props} />,
                              blockquote: ({node, ...props}) => (
                                <blockquote className="prose-blockquote" {...props} />
                              )
                            }}
                          >
                            {msg.content}
                          </ReactMarkdown>
                          {isStreaming && msg.id === messages[messages.length - 1].id && (
                            <span className="typing-cursor"></span>
                          )}
                        </div>
                      )}
                    </div>
                    <span className="message-time">{msg.timestamp}</span>
                  </div>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} className="scroll-anchor" />
          </div>

          {/* Input Box */}
          <div className="input-container">
            <div className="input-wrapper">
              <form onSubmit={handleSubmit} className="input-form">
                <textarea
                  ref={inputRef}
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Give NERVE a task to execute..."
                  className="input-textarea"
                  rows={1}
                />
                <button
                  type="submit"
                  disabled={!inputValue.trim() || isStreaming}
                  className="send-button"
                >
                  {isStreaming ? <Loader2 size={18} className="spin" /> : <Send size={18} />}
                </button>
              </form>
              <div className="input-hint">
                <span>Enter to send &middot; Shift+Enter for new line</span>
              </div>
            </div>
          </div>
        </main>

        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebar-header">
            <Terminal size={15} />
            <h2 className="sidebar-title">System Activity</h2>
          </div>
          <div className="sidebar-content">
            {tasks.map((task, index) => (
              <div key={task.id} className={`task-item ${index === 0 && isStreaming ? 'task-active' : ''}`}>
                <div className="task-icon">
                  {index === 0 && isStreaming ? <Loader2 size={14} className="spin" /> : task.icon}
                </div>
                <p className="task-text">{task.text}</p>
              </div>
            ))}
          </div>
        </aside>
      </div>

      {/* Styles */}
      <style dangerouslySetInnerHTML={{__html: `
        * {
          box-sizing: border-box;
          margin: 0;
          padding: 0;
        }

        .app-container {
          position: fixed;
          inset: 0;
          display: flex;
          flex-direction: column;
          background-color: #0a0a0a;
          color: #e5e5e5;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          overflow: hidden;
        }

        /* Header */
        .header {
          flex: none;
          height: 56px;
          border-bottom: 1px solid #1a1a1a;
          background-color: #0f0f0f;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 20px;
        }

        .header-brand {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .header-logo {
          width: 32px;
          height: 32px;
          border-radius: 8px;
          background-color: #fafafa;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #0a0a0a;
        }

        .header-text {
          display: flex;
          flex-direction: column;
        }

        .header-title {
          font-size: 15px;
          font-weight: 600;
          color: #fafafa;
          letter-spacing: 0.5px;
        }

        .header-subtitle {
          font-size: 10px;
          color: #666;
          text-transform: uppercase;
          letter-spacing: 1px;
          font-weight: 500;
        }

        .header-status {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 12px;
          border-radius: 20px;
          background-color: rgba(34, 197, 94, 0.1);
          border: 1px solid rgba(34, 197, 94, 0.2);
        }

        .status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: #22c55e;
        }

        .status-text {
          font-size: 11px;
          font-weight: 500;
          color: #22c55e;
        }

        /* Main Container */
        .main-container {
          flex: 1;
          display: flex;
          overflow: hidden;
        }

        /* Chat Panel */
        .chat-panel {
          flex: 1;
          display: flex;
          flex-direction: column;
          min-width: 0;
          overflow: hidden;
        }

        .messages-container {
          flex: 1;
          overflow-y: auto;
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .message-row {
          display: flex;
          width: 100%;
        }

        .message-row-user {
          justify-content: flex-end;
        }

        .message-row-ai {
          justify-content: flex-start;
        }

        .message-wrapper {
          display: flex;
          gap: 12px;
          max-width: 80%;
        }

        .message-wrapper-user {
          flex-direction: row-reverse;
        }

        .message-wrapper-ai {
          flex-direction: row;
        }

        .avatar-container {
          flex-shrink: 0;
          padding-top: 2px;
        }

        .avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .avatar-user {
          background-color: #1a1a1a;
          border: 1px solid #262626;
          color: #a3a3a3;
        }

        .avatar-ai {
          background-color: #171717;
          border: 1px solid #262626;
          color: #fafafa;
        }

        .message-content {
          display: flex;
          flex-direction: column;
          max-width: 100%;
        }

        .message-content-user {
          align-items: flex-end;
        }

        .message-content-ai {
          align-items: flex-start;
        }

        .message-bubble {
          padding: 12px 16px;
          border-radius: 16px;
          font-size: 14px;
          line-height: 1.6;
          max-width: 100%;
          overflow: hidden;
        }

        .bubble-user {
          background-color: #fafafa;
          color: #0a0a0a;
          border-bottom-right-radius: 4px;
          white-space: pre-wrap;
        }

        .bubble-ai {
          background-color: #141414;
          border: 1px solid #1f1f1f;
          color: #e5e5e5;
          border-bottom-left-radius: 4px;
        }

        .message-time {
          font-size: 10px;
          color: #525252;
          margin-top: 6px;
          padding: 0 4px;
        }

        /* Prose Styles */
        .prose-content {
          word-break: break-word;
        }

        .prose-p {
          margin-bottom: 12px;
        }

        .prose-p:last-child {
          margin-bottom: 0;
        }

        .prose-strong {
          font-weight: 600;
          color: #fafafa;
        }

        .prose-h1 {
          font-size: 18px;
          font-weight: 600;
          color: #fafafa;
          margin: 20px 0 12px;
        }

        .prose-h2 {
          font-size: 16px;
          font-weight: 600;
          color: #fafafa;
          margin: 16px 0 10px;
        }

        .prose-h3 {
          font-size: 14px;
          font-weight: 600;
          color: #fafafa;
          margin: 12px 0 8px;
        }

        .prose-list {
          list-style-type: disc;
          margin-left: 20px;
          margin-bottom: 12px;
        }

        .prose-list-ordered {
          list-style-type: decimal;
          margin-left: 20px;
          margin-bottom: 12px;
        }

        .prose-list-item {
          margin-bottom: 4px;
          padding-left: 4px;
        }

        .prose-blockquote {
          border-left: 2px solid #333;
          padding-left: 16px;
          margin: 16px 0;
          color: #737373;
          font-style: italic;
        }

        /* Code Blocks */
        .code-block {
          margin: 12px 0;
          border-radius: 8px;
          overflow: hidden;
          background: #0c0c0c;
          border: 1px solid #1f1f1f;
        }

        .code-header {
          display: flex;
          align-items: center;
          padding: 8px 14px;
          background: #111;
          border-bottom: 1px solid #1f1f1f;
          font-family: 'SF Mono', Monaco, monospace;
          font-size: 11px;
          color: #525252;
          text-transform: lowercase;
        }

        .inline-code {
          background: #1a1a1a;
          color: #d4d4d4;
          padding: 2px 6px;
          border-radius: 4px;
          font-family: 'SF Mono', Monaco, monospace;
          font-size: 13px;
          border: 1px solid #262626;
        }

        .typing-cursor {
          display: inline-block;
          width: 2px;
          height: 16px;
          background-color: #fafafa;
          margin-left: 2px;
          animation: blink 1s infinite;
        }

        @keyframes blink {
          0%, 50% { opacity: 1; }
          51%, 100% { opacity: 0; }
        }

        .scroll-anchor {
          height: 16px;
          flex-shrink: 0;
        }

        /* Input Area */
        .input-container {
          flex: none;
          padding: 16px 24px 24px;
          background: linear-gradient(to top, #0a0a0a 80%, transparent);
        }

        .input-wrapper {
          max-width: 800px;
          margin: 0 auto;
        }

        .input-form {
          display: flex;
          align-items: flex-end;
          gap: 10px;
          background-color: #111;
          border: 1px solid #262626;
          padding: 8px;
          border-radius: 16px;
          transition: border-color 0.2s;
        }

        .input-form:focus-within {
          border-color: #404040;
        }

        .input-textarea {
          flex: 1;
          max-height: 120px;
          min-height: 40px;
          background: transparent;
          color: #e5e5e5;
          font-size: 14px;
          line-height: 1.5;
          resize: none;
          border: none;
          outline: none;
          padding: 8px 12px;
          font-family: inherit;
        }

        .input-textarea::placeholder {
          color: #525252;
        }

        .send-button {
          flex-shrink: 0;
          width: 40px;
          height: 40px;
          border-radius: 12px;
          background-color: #fafafa;
          color: #0a0a0a;
          border: none;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: opacity 0.2s, transform 0.1s;
        }

        .send-button:hover:not(:disabled) {
          opacity: 0.9;
        }

        .send-button:active:not(:disabled) {
          transform: scale(0.96);
        }

        .send-button:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }

        .input-hint {
          text-align: center;
          margin-top: 10px;
        }

        .input-hint span {
          font-size: 11px;
          color: #404040;
        }

        /* Sidebar */
        .sidebar {
          width: 280px;
          display: none;
          flex-direction: column;
          border-left: 1px solid #1a1a1a;
          background-color: #0c0c0c;
        }

        @media (min-width: 768px) {
          .sidebar {
            display: flex;
          }
        }

        .sidebar-header {
          padding: 16px 20px;
          border-bottom: 1px solid #1a1a1a;
          display: flex;
          align-items: center;
          gap: 10px;
          color: #737373;
        }

        .sidebar-title {
          font-size: 11px;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          color: #737373;
        }

        .sidebar-content {
          flex: 1;
          overflow-y: auto;
          padding: 16px;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .task-item {
          display: flex;
          gap: 12px;
          font-size: 12px;
          padding: 12px;
          border-radius: 10px;
          background-color: #111;
          border: 1px solid #1a1a1a;
          color: #525252;
          transition: all 0.2s;
        }

        .task-active {
          background-color: rgba(34, 197, 94, 0.05);
          border-color: rgba(34, 197, 94, 0.2);
          color: #22c55e;
        }

        .task-active .task-icon {
          color: #22c55e;
        }

        .task-icon {
          flex-shrink: 0;
          color: #404040;
        }

        .task-text {
          font-weight: 500;
        }

        /* Scrollbar */
        .messages-container::-webkit-scrollbar,
        .sidebar-content::-webkit-scrollbar {
          width: 6px;
        }

        .messages-container::-webkit-scrollbar-track,
        .sidebar-content::-webkit-scrollbar-track {
          background: transparent;
        }

        .messages-container::-webkit-scrollbar-thumb,
        .sidebar-content::-webkit-scrollbar-thumb {
          background-color: #262626;
          border-radius: 20px;
        }

        .messages-container::-webkit-scrollbar-thumb:hover,
        .sidebar-content::-webkit-scrollbar-thumb:hover {
          background-color: #333;
        }

        /* Spin Animation */
        .spin {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        /* Responsive */
        @media (max-width: 640px) {
          .messages-container {
            padding: 16px;
          }
          
          .message-wrapper {
            max-width: 90%;
          }
          
          .input-container {
            padding: 12px 16px 20px;
          }
        }
      `}} />
    </div>
  );
}