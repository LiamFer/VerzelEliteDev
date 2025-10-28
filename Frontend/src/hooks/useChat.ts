import { useState, useEffect, useCallback } from "react";
import type { AImessage, Message, SchedulingOffer, SchedulingSlot } from "../types/chat";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const STORAGE_KEY = "chat_history";
const API_URL = import.meta.env.VITE_API_URL || "http://localhost:3001";

const sendMessageToAPI = async (message: string, onResponseReceived: () => void): Promise<string | AImessage> => {  const rawResponse = await fetch(`${API_URL}/chat/message`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ message }),
    credentials: "include",
  });

  onResponseReceived();

  const responseBody = await rawResponse.json();

  // Verifica se a resposta HTTP não foi bem-sucedida ou se o corpo contém uma estrutura de erro do backend
  if (!rawResponse.ok || (responseBody && typeof responseBody === 'object' && 'code' in responseBody && 'message' in responseBody)) {
    const errorMessage = responseBody.message || "Ocorreu um erro desconhecido no servidor.";
    throw new Error(errorMessage);
  }

  const aiResponse: AImessage = responseBody;

  if (aiResponse.action === "offer") {
    return {
      action: "offer",
      message: "Encontrei alguns horários disponíveis para você, é só escolher um deles e clicar em agendar!",
      data: aiResponse.data.map((slot: SchedulingSlot) => {
        return {...slot, start_time: new Date(slot.start_time).toISOString()}
      })
    };
  }
  return aiResponse.message;
};

export const useChat = (sessionId: string, onResponseReceived: () => void) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isThinking, setIsThinking] = useState(false);
  const [stompClient, setStompClient] = useState<Client | null>(null);

  // Load chat history from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setMessages(parsed);
      } catch (error) {
        console.error("Failed to parse chat history:", error);
      }
    }
  }, []);

  // Setup WebSocket para receber notificações
  useEffect(() => {
    if (!sessionId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      reconnectDelay: 5000,
      debug: (str) => console.log("STOMP:", str),
    });

    client.onConnect = () => {
      console.log("WebSocket conectado!");
      client.subscribe(`/topic/${sessionId}`, (msg) => {
        if (!msg.body) return;
        try {
          const notification = JSON.parse(msg.body);
          console.log("Notificação recebida:", notification);

          // Adicionar mensagem recebida via WebSocket
          if (notification.message) {
            const notificationMessage: Message = {
              id: `ws-${Date.now()}`,
              role: "assistant",
              content: notification.message,
              timestamp: new Date(),
            };
            setMessages((prev) => [...prev, notificationMessage]);
          }
        } catch (err) {
          console.error("Erro ao processar notificação:", err);
        }
      });
    };

    client.onStompError = (frame) => {
      console.error("STOMP error:", frame);
    };

    client.activate();
    setStompClient(client);

    return () => {
      client.deactivate();
      setStompClient(null);
    };
  }, [sessionId]);

  // Salvar o Chat no Local Storage
  useEffect(() => {
    if (messages.length > 0) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
    }
  }, [messages]);

  const sendMessage = useCallback(async (content: string) => {
    
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: "user",
      content,
      timestamp: new Date(),
    };
    setMessages((prev) => [...prev, userMessage]);
    setIsThinking(true);

    const thinkingMessage: Message = {
      id: `thinking-${Date.now()}`,
      role: "assistant",
      content: "",
      timestamp: new Date(),
      isThinking: true,
    };
    setMessages((prev) => [...prev, thinkingMessage]);

    try {
      const response = await sendMessageToAPI(content, onResponseReceived);

      setMessages((prev) => prev.filter((msg) => !msg.isThinking));

      // Handle response
      if (typeof response === "string") {
        const assistantMessage: Message = {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          content: response,
          timestamp: new Date(),
        };
        setMessages((prev) => [...prev, assistantMessage]);
      } else if (response.action === "offer") {
        const assistantMessage: Message = {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          content: response.message,
          timestamp: new Date(),
          offer: response as SchedulingOffer,
        };
        setMessages((prev) => [...prev, assistantMessage]);
      }
    } catch (error) {
      console.error("Error sending message:", error);
      setMessages((prev) => prev.filter((msg) => !msg.isThinking));
      const errorMessage: Message = {
        id: `error-${Date.now()}`,
        role: "error",
        content: `Oops! Ocorreu um erro. Por favor, tente novamente mais tarde.`,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, errorMessage]);

    } finally {
      setIsThinking(false);
    }
  }, [onResponseReceived]);

  const handleOfferSelection = useCallback((url: string) => {
    window.open(url, "_blank", "noopener,noreferrer");
    setIsThinking(false);
  }, []);

  return {
    messages,
    isThinking,
    sendMessage,
    handleOfferSelection,
  };
};