import { ConfigProvider, theme, Layout } from 'antd';
import WebChat from './components/WebChat';
import './styles/animations.css';
import ColorBends from './components/ColorBends';


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
      <div style={{ position: 'relative', width: '100vw', height: '100vh', backgroundColor: '#0d1117' }}>
        <ColorBends
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            zIndex: 0,
          }}
          colors={["#ff5c7a", "#8a5cff", "#00ffd1"]}
          rotation={0}
          speed={0.2}
          scale={1}
          frequency={1}
          warpStrength={1}
          mouseInfluence={1}
          parallax={0.5}
          noise={0.1}
          transparent
        />
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            backdropFilter: 'blur(30px)',
            zIndex: 1,
          }}
        />
        <Layout style={{ minHeight: '100vh', width: '100%', backgroundColor: 'transparent', position: 'relative', zIndex: 2 }}>
          <Content style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '20px' }}>
            <WebChat sessionId={cookieSessionId} />
          </Content>
        </Layout>
      </div>
    </ConfigProvider>
  );
}

export default App
