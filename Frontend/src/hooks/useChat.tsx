import { useState, useEffect, useCallback } from "react";
import { AImessage, Message, SchedulingOffer, SchedulingSlot } from "../types/chat";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const STORAGE_KEY = "chat_history";

const sendMessageToAPI = async (message: string): Promise<AImessage> => {
    const response: AImessage = await (
        await fetch("http://localhost:3000/chat/message", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ message }),
            credentials: "include",
        })
    ).json();

    if (response.action === "offer" && response.data) {
        return {
            action: "offer",
            message: "Encontrei alguns horários disponíveis para você, só escolher um deles e clique em agendar!",
            data: response.data.map((slot: SchedulingSlot) => {
                return { ...slot, start_time: new Date(slot.start_time).toISOString() }
            })
        };
    }
    return response;
};

export const useChat = (sessionId: string) => {
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
            webSocketFactory: () => new SockJS("http://localhost:3000/ws"),
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

    // Save chat history to localStorage whenever it changes
    useEffect(() => {
        if (messages.length > 0) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
        }
    }, [messages]);

    const sendMessage = useCallback(async (content: string) => {
        // Add user message
        const userMessage: Message = {
            id: `user-${Date.now()}`,
            role: "user",
            content,
            timestamp: new Date(),
        };

        // Add thinking indicator
        const thinkingMessage: Message = {
            id: `thinking-${Date.now()}`,
            role: "assistant",
            content: "",
            timestamp: new Date(),
            isThinking: true,
        };

        setMessages((prev) => [...prev, userMessage, thinkingMessage]);
        setIsThinking(true);

        try {
            const response = await sendMessageToAPI(content);

            let assistantMessage: Message;

            if (response.action === "offer" && response.data) {
                assistantMessage = {
                    id: `assistant-${Date.now()}`,
                    role: "assistant",
                    content: response.message,
                    timestamp: new Date(),
                    offer: {
                        action: "offer",
                        message: response.message,
                        data: response.data,
                    },
                };
            } else {
                assistantMessage = {
                    id: `assistant-${Date.now()}`,
                    role: "assistant",
                    content: response.message,
                    timestamp: new Date(),
                };
            }
            // Replace "thinking" message with the actual response
            setMessages((prev) => [...prev.filter((msg) => !msg.isThinking), assistantMessage]);
        } catch (error) {
            console.error("Error sending message:", error);
            setMessages((prev) => prev.filter((msg) => !msg.isThinking));
        } finally {
            setIsThinking(false);
        }
    }, [isThinking]);

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