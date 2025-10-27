import { useState, useCallback } from 'react';
import { ConfigProvider, theme, Layout } from 'antd';
import WebChat from './components/WebChat';
import './styles/animations.css';
import FaultyTerminal from './components/FaultyTerminal';

const { Content } = Layout;

const getSessionIdFromCookie = () => {
  return document.cookie
    .split("; ")
    .find((row) => row.startsWith("sessionId="))
    ?.split("=")[1] || "";
};

function App() {
  const [sessionId, setSessionId] = useState(getSessionIdFromCookie());

  const handleResponseReceived = useCallback(() => {
    const currentSessionId = getSessionIdFromCookie();
    if (currentSessionId && currentSessionId !== sessionId) {
      setSessionId(currentSessionId);
    }
  }, [sessionId]);

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
      <div style={{ position: 'relative', width: '100vw', height: '100vh', backgroundColor: '#0d1117', overflow: 'hidden' }}>
        <div style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', zIndex: 1 }}>
          <FaultyTerminal
            scale={1.5}
            digitSize={1.2}
            gridMul={[2, 1]}
            timeScale={1}
            pause={false}
            scanlineIntensity={1}
            glitchAmount={1}
            flickerAmount={1}
            noiseAmp={1}
            chromaticAberration={0}
            dither={0}
            curvature={0.1}
            tint='#1677ff'
            mouseReact={true}
            mouseStrength={0.5}
            pageLoadAnimation={false}
            brightness={0.6}
          />
        </div>
        <Layout style={{ minHeight: '100vh', width: '100%', backgroundColor: 'transparent', position: 'relative', zIndex: 2 }}>
          <Content style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '20px' }}>
            <WebChat sessionId={sessionId} onResponseReceived={handleResponseReceived} />
          </Content>
        </Layout>
      </div>
    </ConfigProvider>
  );
}

export default App
