import { List, Typography, Button, Space, Spin, Avatar } from 'antd';
import { ClockCircleOutlined, ApiOutlined } from '@ant-design/icons';
import type { Message } from '../types/chat';

const { Text } = Typography;

interface ChatMessageProps {
    message: Message;
    onOfferSelect: (url: string) => void;
}

const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('pt-BR', {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

const ChatMessage = ({ message, onOfferSelect }: ChatMessageProps) => {
    const isUser = message.role === 'user';

    if (message.isThinking) {
        return (
            <List.Item className="message-in" style={{ display: 'flex', justifyContent: 'flex-start', border: 'none' }}>
                <Space>
                    <Avatar style={{ backgroundColor: 'transparent' }} icon={<ApiOutlined style={{ color: '#1677ff' }} />} />
                    <Spin size="small" />
                    <Text type="secondary">Pensando...</Text>
                </Space>
            </List.Item>
        );
    }

    const assistantMessage = (
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
            <Avatar
                style={{ backgroundColor: '#1677ff', flexShrink: 0, marginTop: '4px' }}
                src="./atlasBot.png"
                size="default"
            />
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                <div
                    style={{
                        background: '#2a2f3b',
                        color: 'white',
                        padding: '8px 12px',
                        borderRadius: '18px',
                        borderTopLeftRadius: '4px',
                    }}
                >
                    <Text style={{ color: 'white' }}>{message.content}</Text>
                </div>
                {message.offer && (
                    <Space wrap className="offer-in" style={{ marginTop: '8px', justifyContent: 'flex-start' }}>
                        {message.offer.data.map((slot) => (
                            <Button
                                key={slot.scheduling_url}
                                icon={<ClockCircleOutlined />}
                                onClick={() => onOfferSelect(slot.scheduling_url)}
                            >
                                {formatTime(slot.start_time)}
                            </Button>
                        ))}
                    </Space>
                )}
            </div>
        </div>
    );

    const userMessage = (
        <div style={{ background: '#1677ff', color: 'white', padding: '8px 12px', borderRadius: '18px', borderTopRightRadius: '4px' }}>
            <Text style={{ color: 'white' }}>{message.content}</Text>
        </div>
    );

    return (
        <List.Item
            className={isUser ? 'message-out' : 'message-in'}
            style={{
                display: 'flex',
                justifyContent: isUser ? 'flex-end' : 'flex-start',
                border: 'none',
                padding: '8px 0',
            }}
        >
            <div
                style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: isUser ? 'flex-end' : 'flex-start',
                    maxWidth: '85%',
                }}
            >
                {isUser ? userMessage : assistantMessage}
                <Text type="secondary" style={{ fontSize: '0.75rem' }}>
                    {new Date(message.timestamp).toLocaleTimeString('pt-BR', {
                        hour: '2-digit',
                        minute: '2-digit',
                    })}
                </Text>
            </div>
        </List.Item>
    );
};

export default ChatMessage;