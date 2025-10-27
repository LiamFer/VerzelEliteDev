import { useState } from 'react';
import { Card, Avatar, Typography, Badge } from 'antd';
import { useChat } from '../hooks/useChat';
import { RobotOutlined } from '@ant-design/icons';
import MessageList from './MessageList';
import ChatInput from './ChatInput';

interface WebChatProps {
    sessionId: string;
    onResponseReceived: () => void;
}

const WebChat = ({ sessionId, onResponseReceived }: WebChatProps) => {
    const [inputValue, setInputValue] = useState('');
    const { messages, isThinking, sendMessage, handleOfferSelection } = useChat(sessionId, onResponseReceived);

    const handleSendMessage = () => {
        if (inputValue.trim()) {
            sendMessage(inputValue);
            setInputValue('');
        }
    };

    const CustomHeader = (
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Badge dot color="green" offset={[4, 33]}>
                <Avatar style={{ backgroundColor: '#1677ff' }} src="./atlasBot.png" size="large" />
            </Badge>
            <div>
                <Typography.Title level={5} style={{ margin: 0, color: 'white' }}>
                    Atlas Bot
                </Typography.Title>
                <Typography.Text type="secondary" style={{ color: '#8c8c8c' }}>Online</Typography.Text>
            </div>
        </div>
    );

    return (
        <Card
            title={CustomHeader}
            bordered={false}
            style={{
                width: '100%',
                maxWidth: '700px',
                height: '80vh',
                maxHeight: '800px',
                display: 'flex',
                flexDirection: 'column',
                boxShadow: '0 8px 24px rgba(0,0,0,0.3)',
            }}
            headStyle={{ background: 'linear-gradient(to right, #2a2f3b, #1e1e1e)', borderBottom: '1px solid #303030', padding: '16px 24px' }}
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