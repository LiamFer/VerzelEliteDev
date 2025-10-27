import { useState } from 'react';
import { Card } from 'antd';
import { useChat } from '../hooks/useChat';
import MessageList from './MessageList';
import ChatInput from './ChatInput';

interface WebChatProps {
    sessionId: string;
}

const WebChat = ({ sessionId }: WebChatProps) => {
    const { messages, isThinking, sendMessage, handleOfferSelection } = useChat(sessionId);
    const [inputValue, setInputValue] = useState('');

    const handleSendMessage = () => {
        if (inputValue.trim()) {
            sendMessage(inputValue);
            setInputValue('');
        }
    };

    return (
        <Card
            title="SDR Webchat"
            bordered={false}
            style={{
                width: '100%',
                maxWidth: '700px',
                height: '80vh',
                display: 'flex',
                flexDirection: 'column',
            }}
            aria-busy={isThinking}
            aria-live="polite"
            bodyStyle={{ flex: 1, padding: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}
        >
            <MessageList messages={messages} onOfferSelect={handleOfferSelection} />
            <ChatInput value={inputValue} onChange={setInputValue} onSend={handleSendMessage} disabled={isThinking} isThinking={isThinking} />
        </Card>
    );
};

export default WebChat;