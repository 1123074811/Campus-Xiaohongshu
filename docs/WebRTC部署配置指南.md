# WebRTC 音频通话部署配置指南

## 部署要求

### 1. HTTPS 要求
WebRTC 需要在安全上下文中运行，生产环境必须使用 HTTPS。

#### 开发环境
- 使用 `localhost` 或 `127.0.0.1` 可以在 HTTP 下测试
- 或者配置本地 HTTPS 证书

#### 生产环境
- 必须配置 SSL 证书
- 使用 HTTPS 协议访问

### 2. 浏览器兼容性
- Chrome 60+
- Firefox 55+
- Safari 11+
- Edge 79+

## 服务器配置

### 1. WebSocket 配置
确保 WebSocket 服务器正常运行：

```yaml
# application.yaml
server:
  port: 9090
  
# 确保防火墙开放 9090 端口
```

### 2. STUN/TURN 服务器配置

#### 使用公共 STUN 服务器（推荐用于开发）
```javascript
const rtcConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun.stunprotocol.org:3478' }
  ]
}
```

#### 部署私有 TURN 服务器（推荐用于生产）
```bash
# 安装 coturn
sudo apt-get install coturn

# 配置 /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0
external-ip=YOUR_PUBLIC_IP
realm=yourdomain.com
server-name=yourdomain.com
lt-cred-mech
user=username:password
```

### 3. 防火墙配置
```bash
# 开放必要端口
sudo ufw allow 9090/tcp  # WebSocket
sudo ufw allow 3478/tcp  # STUN
sudo ufw allow 3478/udp  # STUN
sudo ufw allow 5349/tcp  # TURNS
sudo ufw allow 49152:65535/udp  # RTP/RTCP
```

## 前端配置

### 1. 修改 WebRTC 配置
根据实际部署环境修改 STUN/TURN 服务器配置：

```javascript
// 在 Chat.vue 中修改
const rtcConfiguration = {
  iceServers: [
    { urls: 'stun:your-stun-server.com:3478' },
    {
      urls: 'turn:your-turn-server.com:3478',
      username: 'your-username',
      credential: 'your-password'
    }
  ]
}
```

### 2. 修改 WebSocket 连接地址
```javascript
// 根据部署环境修改
const socketUrl = "wss://yourdomain.com:9090/chatServer/" + userId;
// 或者
const socketUrl = "ws://localhost:9090/chatServer/" + userId;
```

## 性能优化

### 1. 音频编解码器优化
```javascript
// 在创建 RTCPeerConnection 时添加
const offer = await peerConnection.createOffer({
  offerToReceiveAudio: true,
  offerToReceiveVideo: false
});

// 优化 SDP 以使用特定编解码器
const optimizedSdp = offer.sdp.replace(
  'm=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 0 8 106 105 13 110 112 113 126',
  'm=audio 9 UDP/TLS/RTP/SAVPF 111'  // 优先使用 Opus 编解码器
);
```

### 2. 连接质量监控
```javascript
// 添加连接状态监控
peerConnection.onconnectionstatechange = () => {
  console.log('Connection state:', peerConnection.connectionState);
  if (peerConnection.connectionState === 'failed') {
    // 处理连接失败
    handleConnectionFailure();
  }
};

// 添加 ICE 连接状态监控
peerConnection.oniceconnectionstatechange = () => {
  console.log('ICE connection state:', peerConnection.iceConnectionState);
};
```

## 监控和日志

### 1. 后端日志配置
```yaml
# application.yaml
logging:
  level:
    com.example.springboot.common.WebSocketServer: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 2. 前端错误监控
```javascript
// 添加全局错误处理
window.addEventListener('unhandledrejection', event => {
  console.error('WebRTC Promise rejection:', event.reason);
  // 发送错误报告到监控系统
});

// WebRTC 特定错误处理
peerConnection.onerror = (error) => {
  console.error('WebRTC error:', error);
  // 记录错误并尝试恢复
};
```

## 安全配置

### 1. CSP 配置
```html
<!-- 在 index.html 中添加 -->
<meta http-equiv="Content-Security-Policy" 
      content="default-src 'self'; 
               connect-src 'self' wss: ws:; 
               media-src 'self' blob:;">
```

### 2. 权限策略
```html
<!-- 确保麦克风权限策略正确 -->
<meta http-equiv="Permissions-Policy" 
      content="microphone=(self)">
```

## 故障排除

### 1. 常见部署问题

#### WebSocket 连接失败
```bash
# 检查端口是否开放
netstat -tlnp | grep 9090

# 检查防火墙状态
sudo ufw status

# 检查服务是否运行
ps aux | grep java
```

#### HTTPS 证书问题
```bash
# 检查证书有效性
openssl s_client -connect yourdomain.com:443

# 检查证书链
curl -I https://yourdomain.com
```

### 2. 性能问题诊断

#### 网络延迟测试
```javascript
// 添加 RTT 测量
const startTime = Date.now();
peerConnection.getStats().then(stats => {
  stats.forEach(report => {
    if (report.type === 'candidate-pair' && report.state === 'succeeded') {
      console.log('RTT:', report.currentRoundTripTime * 1000, 'ms');
    }
  });
});
```

#### 音频质量监控
```javascript
// 监控音频统计
setInterval(() => {
  peerConnection.getStats().then(stats => {
    stats.forEach(report => {
      if (report.type === 'inbound-rtp' && report.mediaType === 'audio') {
        console.log('Audio packets lost:', report.packetsLost);
        console.log('Audio jitter:', report.jitter);
      }
    });
  });
}, 5000);
```

## 扩展配置

### 1. 负载均衡
```nginx
# Nginx 配置示例
upstream websocket {
    server 127.0.0.1:9090;
    server 127.0.0.1:9091;
}

server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    location /chatServer/ {
        proxy_pass http://websocket;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

### 2. Redis 集群支持
```java
// 如果需要支持多实例部署，可以使用 Redis 存储会话信息
@Component
public class RedisWebSocketSessionManager {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void storeSession(Integer userId, String sessionId) {
        redisTemplate.opsForValue().set("session:" + userId, sessionId);
    }
    
    public String getSession(Integer userId) {
        return (String) redisTemplate.opsForValue().get("session:" + userId);
    }
}
```