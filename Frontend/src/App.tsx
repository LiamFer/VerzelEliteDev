import { ConfigProvider, theme, Layout } from 'antd';
import WebChat from './components/WebChat';
import './animations.css';

const { Content } = Layout;

function App() {
  const cookieSessionId = document.cookie
    .split("; ")
    .find((row) => row.startsWith("sessionId="))
    ?.split("=")[1] || "";

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#1677ff',
          colorBgBase: '#0d1117',
          colorBgContainer: '#161b22',
        },
      }}
    >
      <Layout style={{ minHeight: '100vh', width: '100%', backgroundColor: '#0d1117' }}>
        <Content style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '20px' }}>
          <WebChat sessionId={cookieSessionId} />
        </Content>
      </Layout>
    </ConfigProvider>
  );
}

export default App
