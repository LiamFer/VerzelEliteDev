import { Input, Button, Space } from 'antd';
import { SendOutlined } from '@ant-design/icons';

interface ChatInputProps {
    value: string;
    onChange: (value: string) => void;
    onSend: () => void;
    disabled: boolean;
    isThinking: boolean;
}

const ChatInput = ({ value, onChange, onSend, disabled, isThinking }: ChatInputProps) => {
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && !disabled) {
            onSend();
        }
        if (e.key === 'Escape') {
            onChange('');
        }
    };

    return (
        <div style={{ padding: '16px 24px', borderTop: '1px solid #303030' }}>
            <Space.Compact style={{ width: '100%' }}>
                <Input
                    aria-label="Digite sua mensagem"
                    placeholder={isThinking ? 'Aguarde o assistente responder...' : 'Digite sua mensagem...'}
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    onKeyDown={handleKeyDown}
                    disabled={disabled}
                    size="large"
                />
                <Button type="primary" icon={<SendOutlined />} onClick={onSend} disabled={disabled || !value.trim()} size="large" />
            </Space.Compact>
        </div>
    );
};

export default ChatInput;