import { useEffect, useRef } from 'react';
import { List } from 'antd';
import ChatMessage from './ChatMessage';
import type { Message } from '../types/chat';

interface MessageListProps {
    messages: Message[];
    onOfferSelect: (url: string) => void;
}

const MessageList = ({ messages, onOfferSelect }: MessageListProps) => {
    const listEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div role="log" aria-live="polite" style={{ flex: 1, overflowY: 'auto', padding: '16px 24px' }}>
            <List
                dataSource={messages}
                renderItem={(item) => <ChatMessage message={item} onOfferSelect={onOfferSelect} />}
                split={false}
            />
            <div ref={listEndRef} />
        </div>
    );
};

export default MessageList;