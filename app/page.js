"use client"
import React, { useState, useRef, useEffect } from 'react';
import {
  Send,
  Cpu,
  User,
  Sparkles,
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
    messagesEndRef.current?.scrollIntoView({
      behavior: 'auto',
      block: 'end'
    });
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

    // Use a ref-safe ID so the closure always has the right value
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

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder("utf-8");
      
      // 1. Initialize a buffer to hold cross-chunk data
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // 2. Add new data to the buffer
        buffer += decoder.decode(value, { stream: true });
        let tokensInChunk = '';

        // 3. Process the buffer ONLY when it contains a complete event (\n\n)
        // 3. Process the buffer ONLY when it contains a complete event (\n\n)
        while (buffer.includes('\n\n')) {
          const eventEndIndex = buffer.indexOf('\n\n');
          // Extract the complete event block
          const eventBlock = buffer.slice(0, eventEndIndex);
          // Remove the processed event block and the \n\n from the buffer
          buffer = buffer.slice(eventEndIndex + 2);

          // SSE blocks can contain multiple lines, process them individually
          const lines = eventBlock.split('\n');
          for (const line of lines) {
            if (line.startsWith('data:')) {
              // Simply slice off "data:" (exactly 5 characters).
              // We removed the space-stripping logic because your backend
              // attaches the raw token (including its spaces) directly to the colon.
              const token = line.slice(5);
              
              tokensInChunk += token;
            }
          }
        }

        // 4. Update state with all complete tokens extracted in this pass
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
    <div className="fixed inset-0 flex flex-col bg-[#0B0F19] text-slate-200 font-sans overflow-hidden selection:bg-pink-500/30">
      <div className="absolute top-[-10%] left-[-10%] w-[40vw] h-[40vw] bg-pink-600/20 blur-[120px] rounded-full mix-blend-screen animate-blob pointer-events-none"></div>
      <div className="absolute top-[20%] right-[-10%] w-[35vw] h-[35vw] bg-blue-600/20 blur-[120px] rounded-full mix-blend-screen animate-blob animation-delay-2000 pointer-events-none"></div>
      <div className="absolute bottom-[-20%] left-[20%] w-[45vw] h-[45vw] bg-purple-600/20 blur-[120px] rounded-full mix-blend-screen animate-blob animation-delay-4000 pointer-events-none"></div>
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none"></div>

      {/* Header */}
      <header className="flex-none h-16 border-b border-white/10 bg-slate-900/50 backdrop-blur-md flex items-center justify-between px-6 z-10 relative">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-pink-500 to-blue-600 flex items-center justify-center shadow-lg shadow-pink-500/20">
            <Sparkles className="text-white" size={18} />
          </div>
          <div>
            <h1 className="text-lg font-bold bg-gradient-to-r from-blue-400 to-pink-500 bg-clip-text text-transparent tracking-wide">NERVE</h1>
            <p className="text-[10px] text-slate-400 uppercase tracking-wider font-semibold">Local Orchestrator</p>
          </div>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20">
          <div className="w-2 h-2 rounded-full bg-blue-400 animate-pulse"></div>
          <span className="text-xs font-medium text-blue-300">Llama 3.2 3B</span>
        </div>
      </header>

      {/* Main */}
      <div className="flex flex-1 overflow-hidden z-10 relative">

        {/* Chat */}
        <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 custom-scrollbar">
            {messages.map((msg) => (
              <div key={msg.id} className={`flex w-full ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`flex gap-3 max-w-[85%] md:max-w-[75%] ${msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                  <div className="flex-shrink-0 mt-1">
                    {msg.role === 'user' ? (
                      <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center">
                        <User size={16} className="text-blue-400" />
                      </div>
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-gradient-to-br from-pink-500/20 to-blue-600/20 border border-pink-500/30 flex items-center justify-center">
                        <Cpu size={16} className="text-pink-400" />
                      </div>
                    )}
                  </div>
                  <div className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                    <div className={`px-5 py-3.5 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap shadow-lg
                      ${msg.role === 'user'
                        ? 'bg-gradient-to-br from-blue-600 to-indigo-700 text-white rounded-tr-sm'
                        : 'bg-slate-800/80 backdrop-blur-sm border border-slate-700 text-slate-200 rounded-tl-sm'
                      }`}>
                      {msg.content}
                      {msg.role === 'ai' && isStreaming && msg.id === messages[messages.length - 1].id && (
                        <span className="inline-block w-1.5 h-4 ml-1 align-middle bg-pink-500 animate-pulse"></span>
                      )}
                    </div>
                    <span className="text-[10px] text-slate-500 mt-1.5 px-1">{msg.timestamp}</span>
                  </div>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} className="h-4 flex-shrink-0" />
          </div>

          {/* Input */}
          <div className="flex-none p-4 bg-gradient-to-t from-[#0B0F19] via-[#0B0F19] to-transparent pt-10 z-20 relative">
            <div className="max-w-4xl mx-auto relative group">
              <div className="absolute -inset-0.5 bg-gradient-to-r from-pink-500 to-blue-500 rounded-2xl blur opacity-20 group-hover:opacity-40 transition duration-500"></div>
              <form onSubmit={handleSubmit} className="relative flex items-end gap-2 bg-slate-900/90 backdrop-blur-xl border border-slate-700 p-2 rounded-2xl shadow-xl">
                <textarea
                  ref={inputRef}
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Give NERVE a task to execute..."
                  className="w-full max-h-32 min-h-[44px] bg-transparent text-slate-200 placeholder-slate-500 text-sm resize-none focus:outline-none p-3 custom-scrollbar"
                  rows={1}
                />
                <button
                  type="submit"
                  disabled={!inputValue.trim() || isStreaming}
                  className="flex-shrink-0 p-3 rounded-xl bg-gradient-to-br from-pink-500 to-blue-600 text-white hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-95"
                >
                  {isStreaming ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} />}
                </button>
              </form>
              <div className="text-center mt-2">
                <span className="text-[10px] text-slate-500">Enter to send · Shift+Enter for new line · Type "bye" to save memory</span>
              </div>
            </div>
          </div>
        </main>

        {/* Sidebar */}
        <aside className="w-72 lg:w-80 hidden md:flex flex-col border-l border-white/5 bg-slate-900/30 backdrop-blur-md">
          <div className="p-4 border-b border-white/5 flex items-center gap-2">
            <Terminal size={16} className="text-pink-400" />
            <h2 className="text-xs font-bold uppercase tracking-widest text-slate-300">System Activity</h2>
          </div>
          <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
            {tasks.map((task, index) => (
              <div key={task.id} className={`flex gap-3 text-sm p-3 rounded-xl border ${
                index === 0 && isStreaming
                  ? 'bg-blue-900/10 border-blue-500/30 text-blue-200'
                  : 'bg-slate-800/30 border-slate-800 text-slate-400'
              } transition-colors`}>
                <div className={`mt-0.5 ${index === 0 && isStreaming ? 'text-blue-400' : 'text-slate-500'}`}>
                  {index === 0 && isStreaming ? <Loader2 size={14} className="animate-spin" /> : task.icon}
                </div>
                <p className="text-xs font-medium">{task.text}</p>
              </div>
            ))}
          </div>
        </aside>
      </div>

      <style dangerouslySetInnerHTML={{__html: `
        @keyframes blob {
          0% { transform: translate(0px, 0px) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
          100% { transform: translate(0px, 0px) scale(1); }
        }
        .animate-blob { animation: blob 7s infinite; }
        .animation-delay-2000 { animation-delay: 2s; }
        .animation-delay-4000 { animation-delay: 4s; }
        .custom-scrollbar::-webkit-scrollbar { width: 6px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background-color: rgba(255,255,255,0.1); border-radius: 20px; }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover { background-color: rgba(255,255,255,0.2); }
      `}} />
    </div>
  );
}