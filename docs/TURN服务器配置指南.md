# TURN服务器配置指南 - Linux环境WebRTC解决方案

## 问题背景

在Linux服务器环境中，由于严格的网络配置和防火墙限制，STUN服务器往往无法有效穿透NAT，导致WebRTC连接失败。TURN服务器作为中继服务器，可以在无法建立直接P2P连接时提供数据转发服务。

## 解决方案概述

### STUN vs TURN
- **STUN服务器**: 帮助客户端发现自己的公网IP和端口，用于NAT穿透
- **TURN服务器**: 当NAT穿透失败时，作为中继服务器转发数据流

### 部署架构
```
客户端A ←→ TURN服务器 ←→ 客户端B
```

## 1. 安装coturn服务器

### 方案一：Docker部署（推荐）

#### 1.1 使用官方Docker镜像
```bash
# 拉取官方coturn镜像
docker pull coturn/coturn:latest

# 创建配置目录
mkdir -p /opt/coturn/config
mkdir -p /opt/coturn/logs
mkdir -p /opt/coturn/data
```

#### 1.2 创建Docker配置文件
```bash
# 创建turnserver.conf配置文件
cat > /opt/coturn/config/turnserver.conf << 'EOF'
# Docker环境TURN服务器配置
listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0

# 外网IP（需要替换为实际公网IP）
external-ip=YOUR_PUBLIC_IP

# 域名配置
realm=turn.yourdomain.com
server-name=turn.yourdomain.com

# 启用长期凭证机制
lt-cred-mech

# 用户认证
user=webrtc:SecurePassword123!

# 日志配置
log-file=/var/log/turnserver.log
verbose

# 安全配置
fingerprint
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1

# 端口范围配置
min-port=49152
max-port=65535

# 禁止访问私有网络
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=127.0.0.0-127.255.255.255
denied-peer-ip=0.0.0.0-0.255.255.255
denied-peer-ip=169.254.0.0-169.254.255.255

# 性能优化
total-quota=100
bps-capacity=0
stale-nonce=600
EOF
```

#### 1.3 Docker Compose部署
```yaml
# 创建docker-compose.yml文件
cat > /opt/coturn/docker-compose.yml << 'EOF'
version: '3.8'

services:
  coturn:
    image: coturn/coturn:latest
    container_name: coturn-server
    restart: unless-stopped
    
    # 网络配置
    network_mode: host
    
    # 端口映射（如果不使用host网络模式）
    # ports:
    #   - "3478:3478/tcp"
    #   - "3478:3478/udp"
    #   - "5349:5349/tcp"
    #   - "5349:5349/udp"
    #   - "49152-65535:49152-65535/udp"
    
    # 卷挂载
    volumes:
      - ./config/turnserver.conf:/etc/coturn/turnserver.conf:ro
      - ./logs:/var/log
      - ./data:/var/lib/coturn
    
    # 环境变量
    environment:
      - TURN_USERNAME=webrtc
      - TURN_PASSWORD=SecurePassword123!
      - TURN_REALM=turn.yourdomain.com
      - TURN_EXTERNAL_IP=YOUR_PUBLIC_IP
    
    # 启动命令
    command: [
      "-c", "/etc/coturn/turnserver.conf",
      "--log-file=/var/log/turnserver.log",
      "--pidfile=/var/run/turnserver.pid"
    ]
    
    # 健康检查
    healthcheck:
      test: ["CMD", "turnutils_peer", "-n", "-r", "localhost", "-u", "webrtc", "-w", "SecurePassword123!"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # 可选：添加监控服务
  coturn-exporter:
    image: prom/node-exporter:latest
    container_name: coturn-exporter
    restart: unless-stopped
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
EOF
```

#### 1.4 启动Docker服务
```bash
# 进入配置目录
cd /opt/coturn

# 替换配置文件中的实际IP
PUBLIC_IP=$(curl -s ifconfig.me)
sed -i "s/YOUR_PUBLIC_IP/$PUBLIC_IP/g" config/turnserver.conf
sed -i "s/YOUR_PUBLIC_IP/$PUBLIC_IP/g" docker-compose.yml

# 启动服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f coturn
```

#### 1.5 Docker单容器部署
```bash
# 直接使用docker run命令
docker run -d \
  --name coturn-server \
  --restart unless-stopped \
  --network host \
  -v /opt/coturn/config/turnserver.conf:/etc/coturn/turnserver.conf:ro \
  -v /opt/coturn/logs:/var/log \
  -v /opt/coturn/data:/var/lib/coturn \
  coturn/coturn:latest \
  -c /etc/coturn/turnserver.conf
```

#### 1.6 自定义Docker镜像
```dockerfile
# 创建自定义Dockerfile
cat > /opt/coturn/Dockerfile << 'EOF'
FROM ubuntu:22.04

# 安装依赖
RUN apt-get update && apt-get install -y \
    coturn \
    curl \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

# 创建必要目录
RUN mkdir -p /var/log /var/lib/coturn /etc/coturn

# 复制配置文件
COPY config/turnserver.conf /etc/coturn/turnserver.conf

# 创建启动脚本
RUN echo '#!/bin/bash\n\
# 替换环境变量\n\
if [ ! -z "$TURN_EXTERNAL_IP" ]; then\n\
    sed -i "s/YOUR_PUBLIC_IP/$TURN_EXTERNAL_IP/g" /etc/coturn/turnserver.conf\n\
fi\n\
if [ ! -z "$TURN_REALM" ]; then\n\
    sed -i "s/turn.yourdomain.com/$TURN_REALM/g" /etc/coturn/turnserver.conf\n\
fi\n\
if [ ! -z "$TURN_USERNAME" ] && [ ! -z "$TURN_PASSWORD" ]; then\n\
    sed -i "s/webrtc:SecurePassword123!/$TURN_USERNAME:$TURN_PASSWORD/g" /etc/coturn/turnserver.conf\n\
fi\n\
\n\
# 启动turnserver\n\
exec turnserver "$@"' > /usr/local/bin/start-turnserver.sh

RUN chmod +x /usr/local/bin/start-turnserver.sh

# 暴露端口
EXPOSE 3478/tcp 3478/udp 5349/tcp 5349/udp
EXPOSE 49152-65535/udp

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD turnutils_peer -n -r localhost -u ${TURN_USERNAME:-webrtc} -w ${TURN_PASSWORD:-SecurePassword123!} || exit 1

# 启动命令
ENTRYPOINT ["/usr/local/bin/start-turnserver.sh"]
CMD ["-c", "/etc/coturn/turnserver.conf", "--log-file=/var/log/turnserver.log"]
EOF

# 构建自定义镜像
docker build -t custom-coturn:latest /opt/coturn/

# 运行自定义镜像
docker run -d \
  --name coturn-custom \
  --restart unless-stopped \
  --network host \
  -e TURN_EXTERNAL_IP=$(curl -s ifconfig.me) \
  -e TURN_REALM=turn.yourdomain.com \
  -e TURN_USERNAME=webrtc \
  -e TURN_PASSWORD=SecurePassword123! \
  -v /opt/coturn/logs:/var/log \
  custom-coturn:latest
```

### 方案二：传统安装

#### 2.1 Ubuntu/Debian系统
```bash
# 更新包管理器
sudo apt update

# 安装coturn
sudo apt install coturn

# 启用coturn服务
sudo systemctl enable coturn
```

#### 2.2 CentOS/RHEL系统
```bash
# 安装EPEL仓库
sudo yum install epel-release

# 安装coturn
sudo yum install coturn

# 启用coturn服务
sudo systemctl enable coturn
```

#### 2.3 从源码编译（推荐用于生产环境）
```bash
# 安装依赖
sudo apt install build-essential libssl-dev libevent-dev libsqlite3-dev pkg-config

# 下载源码
wget https://github.com/coturn/coturn/archive/refs/tags/4.6.2.tar.gz
tar -xzf 4.6.2.tar.gz
cd coturn-4.6.2

# 编译安装
./configure
make
sudo make install
```

## 2. 配置coturn服务器

### 创建配置文件
```bash
sudo nano /etc/turnserver.conf
```

### 基础配置
```conf
# /etc/turnserver.conf

# 监听端口
listening-port=3478
tls-listening-port=5349

# 监听IP（设置为服务器的内网IP）
listening-ip=0.0.0.0

# 外网IP（设置为服务器的公网IP）
external-ip=YOUR_PUBLIC_IP

# 域名配置
realm=yourdomain.com
server-name=yourdomain.com

# 启用长期凭证机制
lt-cred-mech

# 用户认证（用户名:密码）
user=turnuser:turnpassword

# 数据库配置（可选，用于用户管理）
# userdb=/var/lib/turn/turndb

# 日志配置
log-file=/var/log/turnserver.log
verbose

# 安全配置
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1

# 端口范围配置
min-port=49152
max-port=65535

# 禁止访问私有网络
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=172.16.0.0-172.31.255.255

# SSL证书配置（HTTPS环境必需）
# cert=/etc/ssl/certs/turn_server_cert.pem
# pkey=/etc/ssl/private/turn_server_pkey.pem
```

### 生产环境配置示例
```conf
# 生产环境配置
listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0
external-ip=YOUR_PUBLIC_IP/YOUR_PRIVATE_IP

realm=turn.yourdomain.com
server-name=turn.yourdomain.com

lt-cred-mech
user=webrtc:SecurePassword123!

# 使用SQLite数据库
userdb=/var/lib/turn/turndb

# 日志配置
log-file=/var/log/turnserver.log
syslog

# 性能优化
total-quota=100
bps-capacity=0
stale-nonce=600

# 安全配置
fingerprint
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1
cipher-list="ECDH+AESGCM:DH+AESGCM:ECDH+AES256:DH+AES256:ECDH+AES128:DH+AES:ECDH+3DES:DH+3DES:RSA+AESGCM:RSA+AES:RSA+3DES:!aNULL:!MD5:!DSS"

# 端口范围
min-port=49152
max-port=65535

# 网络限制
denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=127.0.0.0-127.255.255.255
denied-peer-ip=0.0.0.0-0.255.255.255
denied-peer-ip=169.254.0.0-169.254.255.255
```

## 3. 防火墙配置

### UFW防火墙（Ubuntu）
```bash
# 开放TURN服务端口
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp
sudo ufw allow 5349/udp

# 开放RTP端口范围
sudo ufw allow 49152:65535/udp

# 重新加载防火墙
sudo ufw reload
```

### iptables防火墙
```bash
# 开放TURN端口
sudo iptables -A INPUT -p tcp --dport 3478 -j ACCEPT
sudo iptables -A INPUT -p udp --dport 3478 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 5349 -j ACCEPT
sudo iptables -A INPUT -p udp --dport 5349 -j ACCEPT

# 开放RTP端口范围
sudo iptables -A INPUT -p udp --dport 49152:65535 -j ACCEPT

# 保存规则
sudo iptables-save > /etc/iptables/rules.v4
```

### firewalld防火墙（CentOS/RHEL）
```bash
# 开放端口
sudo firewall-cmd --permanent --add-port=3478/tcp
sudo firewall-cmd --permanent --add-port=3478/udp
sudo firewall-cmd --permanent --add-port=5349/tcp
sudo firewall-cmd --permanent --add-port=5349/udp
sudo firewall-cmd --permanent --add-port=49152-65535/udp

# 重新加载
sudo firewall-cmd --reload
```

## 4. SSL证书配置（生产环境）

### 使用Let's Encrypt
```bash
# 安装certbot
sudo apt install certbot

# 获取证书
sudo certbot certonly --standalone -d turn.yourdomain.com

# 配置证书路径
cert=/etc/letsencrypt/live/turn.yourdomain.com/fullchain.pem
pkey=/etc/letsencrypt/live/turn.yourdomain.com/privkey.pem
```

### 自签名证书（测试环境）
```bash
# 生成私钥
sudo openssl genrsa -out /etc/ssl/private/turn_server_pkey.pem 2048

# 生成证书
sudo openssl req -new -x509 -key /etc/ssl/private/turn_server_pkey.pem \
    -out /etc/ssl/certs/turn_server_cert.pem -days 365
```

## 5. 启动和管理服务

### Docker环境管理

#### 5.1 Docker Compose管理
```bash
# 启动服务
cd /opt/coturn
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f coturn

# 更新服务
docker-compose pull
docker-compose up -d
```

#### 5.2 Docker容器管理
```bash
# 启动容器
docker start coturn-server

# 停止容器
docker stop coturn-server

# 重启容器
docker restart coturn-server

# 查看容器状态
docker ps | grep coturn

# 查看容器日志
docker logs -f coturn-server

# 进入容器调试
docker exec -it coturn-server /bin/bash

# 查看容器资源使用
docker stats coturn-server
```

#### 5.3 Docker服务管理脚本
```bash
#!/bin/bash
# /usr/local/bin/docker-coturn-manager.sh

COMPOSE_FILE="/opt/coturn/docker-compose.yml"
CONTAINER_NAME="coturn-server"

case "$1" in
    start)
        cd /opt/coturn && docker-compose up -d
        echo "TURN服务器已启动"
        ;;
    stop)
        cd /opt/coturn && docker-compose down
        echo "TURN服务器已停止"
        ;;
    restart)
        cd /opt/coturn && docker-compose restart
        echo "TURN服务器已重启"
        ;;
    status)
        docker ps | grep $CONTAINER_NAME
        ;;
    logs)
        docker logs -f $CONTAINER_NAME
        ;;
    update)
        cd /opt/coturn
        docker-compose pull
        docker-compose up -d
        echo "TURN服务器已更新"
        ;;
    shell)
        docker exec -it $CONTAINER_NAME /bin/bash
        ;;
    stats)
        docker stats $CONTAINER_NAME
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs|update|shell|stats}"
        exit 1
        ;;
esac
```

### 传统系统服务管理

#### 5.4 SystemD服务管理
```bash
# 启动服务
sudo systemctl start coturn

# 检查状态
sudo systemctl status coturn

# 查看日志
sudo journalctl -u coturn -f

# 设置开机自启
sudo systemctl enable coturn
```

#### 5.5 传统服务管理脚本
```bash
#!/bin/bash
# /usr/local/bin/coturn-manager.sh

case "$1" in
    start)
        sudo systemctl start coturn
        echo "TURN服务器已启动"
        ;;
    stop)
        sudo systemctl stop coturn
        echo "TURN服务器已停止"
        ;;
    restart)
        sudo systemctl restart coturn
        echo "TURN服务器已重启"
        ;;
    status)
        sudo systemctl status coturn
        ;;
    logs)
        sudo journalctl -u coturn -f
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs}"
        exit 1
        ;;
esac
```

## 6. 前端配置更新

### 修改WebRTC配置
```javascript
// 在Chat.vue中更新TURN服务器配置
const rtcConfiguration = {
  iceServers: [
    // STUN服务器（用于NAT穿透）
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    
    // TURN服务器（用于中继）
    {
      urls: 'turn:YOUR_SERVER_IP:3478',
      username: 'webrtc',
      credential: 'SecurePassword123!'
    },
    {
      urls: 'turns:YOUR_SERVER_IP:5349',
      username: 'webrtc',
      credential: 'SecurePassword123!'
    }
  ],
  iceCandidatePoolSize: 10
}
```

### 环境配置文件
```javascript
// config/webrtc.js
const webrtcConfig = {
  development: {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      {
        urls: 'turn:localhost:3478',
        username: 'testuser',
        credential: 'testpass'
      }
    ]
  },
  production: {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      {
        urls: 'turn:turn.yourdomain.com:3478',
        username: 'webrtc',
        credential: 'SecurePassword123!'
      },
      {
        urls: 'turns:turn.yourdomain.com:5349',
        username: 'webrtc',
        credential: 'SecurePassword123!'
      }
    ]
  }
}

export default webrtcConfig[process.env.NODE_ENV || 'development']
```

## 7. 测试和验证

### 测试TURN服务器连通性
```bash
# 使用turnutils测试工具
turnutils_peer -n -r turn.yourdomain.com -u webrtc -w SecurePassword123!

# 测试UDP连接
turnutils_uclient -t -u webrtc -w SecurePassword123! turn.yourdomain.com

# 测试TCP连接
turnutils_tclient -t -u webrtc -w SecurePassword123! turn.yourdomain.com
```

### 在线测试工具
访问以下网站测试TURN服务器：
- https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/
- https://test.webrtc.org/

### 浏览器调试
```javascript
// 在浏览器控制台中测试
const pc = new RTCPeerConnection({
  iceServers: [
    {
      urls: 'turn:YOUR_SERVER_IP:3478',
      username: 'webrtc',
      credential: 'SecurePassword123!'
    }
  ]
});

pc.onicecandidate = (event) => {
  if (event.candidate) {
    console.log('ICE候选:', event.candidate);
  }
};

// 创建数据通道触发ICE收集
pc.createDataChannel('test');
pc.createOffer().then(offer => pc.setLocalDescription(offer));
```

## 8. 监控和维护

### Docker环境监控

#### 8.1 Docker日志监控
```bash
# 实时监控Docker容器日志
docker logs -f coturn-server

# 查看最近的日志
docker logs --tail 100 coturn-server

# 分析连接统计
docker exec coturn-server grep "session" /var/log/turnserver.log | tail -20

# 监控错误
docker exec coturn-server grep "ERROR" /var/log/turnserver.log
```

#### 8.2 Docker性能监控脚本
```bash
#!/bin/bash
# /usr/local/bin/docker-turn-monitor.sh

CONTAINER_NAME="coturn-server"

echo "=== Docker TURN服务器状态监控 ==="
echo "时间: $(date)"
echo

# 检查容器状态
echo "容器状态:"
docker ps | grep $CONTAINER_NAME

# 检查容器资源使用
echo -e "\n容器资源使用:"
docker stats --no-stream $CONTAINER_NAME

# 检查端口监听
echo -e "\n端口监听:"
docker exec $CONTAINER_NAME netstat -tulpn | grep :3478
docker exec $CONTAINER_NAME netstat -tulpn | grep :5349

# 检查连接数
echo -e "\n当前连接数:"
docker exec $CONTAINER_NAME netstat -an | grep :3478 | wc -l

# 检查日志文件大小
echo -e "\n日志文件大小:"
docker exec $CONTAINER_NAME ls -lh /var/log/turnserver.log

# 检查容器健康状态
echo -e "\n健康检查:"
docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>/dev/null || echo "未配置健康检查"
```

#### 8.3 Docker自动重启脚本
```bash
#!/bin/bash
# /usr/local/bin/docker-turn-watchdog.sh

CONTAINER_NAME="coturn-server"
LOG_FILE="/var/log/docker-turn-watchdog.log"

# 检查容器是否运行
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo "$(date): TURN容器未运行，正在重启..." >> $LOG_FILE
    
    # 尝试启动容器
    cd /opt/coturn && docker-compose up -d
    sleep 15
    
    # 检查启动结果
    if docker ps | grep -q $CONTAINER_NAME; then
        echo "$(date): TURN容器重启成功" >> $LOG_FILE
    else
        echo "$(date): TURN容器重启失败" >> $LOG_FILE
        # 发送告警通知（可选）
        # curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
        #      -d "chat_id=<CHAT_ID>&text=TURN服务器重启失败"
    fi
fi

# 检查容器健康状态
HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>/dev/null)
if [ "$HEALTH_STATUS" = "unhealthy" ]; then
    echo "$(date): TURN容器健康检查失败，正在重启..." >> $LOG_FILE
    docker restart $CONTAINER_NAME
fi
```

#### 8.4 Docker Compose监控配置
```yaml
# 在docker-compose.yml中添加监控服务
version: '3.8'

services:
  coturn:
    # ... 现有配置 ...
    
  # Prometheus监控
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  # Grafana仪表板
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123

volumes:
  prometheus_data:
  grafana_data:
```

### 传统环境监控

#### 8.5 传统日志监控
```bash
# 实时监控日志
sudo tail -f /var/log/turnserver.log

# 分析连接统计
grep "session" /var/log/turnserver.log | tail -20

# 监控错误
grep "ERROR" /var/log/turnserver.log
```

#### 8.6 传统性能监控脚本
```bash
#!/bin/bash
# /usr/local/bin/turn-monitor.sh

echo "=== TURN服务器状态监控 ==="
echo "时间: $(date)"
echo

# 检查服务状态
echo "服务状态:"
systemctl is-active coturn

# 检查端口监听
echo -e "\n端口监听:"
netstat -tulpn | grep :3478
netstat -tulpn | grep :5349

# 检查连接数
echo -e "\n当前连接数:"
netstat -an | grep :3478 | wc -l

# 检查内存使用
echo -e "\n内存使用:"
ps aux | grep turnserver | grep -v grep

# 检查磁盘空间
echo -e "\n日志文件大小:"
ls -lh /var/log/turnserver.log
```

#### 8.7 传统自动重启脚本
```bash
#!/bin/bash
# /usr/local/bin/turn-watchdog.sh

# 检查TURN服务器是否运行
if ! systemctl is-active --quiet coturn; then
    echo "$(date): TURN服务器未运行，正在重启..." >> /var/log/turn-watchdog.log
    systemctl restart coturn
    sleep 10
    
    if systemctl is-active --quiet coturn; then
        echo "$(date): TURN服务器重启成功" >> /var/log/turn-watchdog.log
    else
        echo "$(date): TURN服务器重启失败" >> /var/log/turn-watchdog.log
    fi
fi
```

### 定时任务配置

#### 8.8 Docker环境定时任务
```bash
# 编辑crontab
sudo crontab -e

# 添加Docker监控任务
*/5 * * * * /usr/local/bin/docker-turn-watchdog.sh
0 2 * * * /usr/local/bin/docker-turn-monitor.sh >> /var/log/docker-turn-monitor.log

# 日志清理任务
0 3 * * 0 docker exec coturn-server find /var/log -name "*.log" -mtime +7 -delete
```

#### 8.9 传统环境定时任务
```bash
# 编辑crontab
sudo crontab -e

# 添加监控任务
*/5 * * * * /usr/local/bin/turn-watchdog.sh
0 2 * * * /usr/local/bin/turn-monitor.sh >> /var/log/turn-monitor.log
```

## 9. 故障排除

### Docker环境故障排除

#### 9.1 Docker容器问题
```bash
# 检查容器状态
docker ps -a | grep coturn

# 查看容器启动日志
docker logs coturn-server

# 检查容器配置
docker inspect coturn-server

# 进入容器调试
docker exec -it coturn-server /bin/bash

# 检查容器网络
docker network ls
docker network inspect bridge
```

#### 9.2 Docker网络问题
```bash
# 检查端口映射
docker port coturn-server

# 检查防火墙（宿主机）
sudo ufw status
sudo iptables -L

# 检查容器内端口监听
docker exec coturn-server netstat -tulpn | grep :3478

# 测试容器网络连通性
docker exec coturn-server ping 8.8.8.8
```

#### 9.3 Docker卷挂载问题
```bash
# 检查卷挂载
docker inspect coturn-server | grep -A 10 "Mounts"

# 检查配置文件权限
ls -la /opt/coturn/config/
docker exec coturn-server ls -la /etc/coturn/

# 修复权限问题
sudo chown -R 999:999 /opt/coturn/logs
sudo chown -R 999:999 /opt/coturn/data
```

#### 9.4 Docker Compose问题
```bash
# 验证compose文件语法
cd /opt/coturn && docker-compose config

# 重新构建服务
docker-compose down
docker-compose up --build -d

# 查看服务依赖
docker-compose ps
docker-compose top
```

### 传统环境故障排除

#### 9.5 连接被拒绝
```bash
# 检查防火墙
sudo ufw status
sudo iptables -L

# 检查端口监听
sudo netstat -tulpn | grep coturn

# 检查服务状态
sudo systemctl status coturn
```

#### 9.6 认证失败
```bash
# 检查用户配置
sudo grep "user=" /etc/turnserver.conf

# 重新生成用户
sudo turnadmin -a -u webrtc -p SecurePassword123! -r yourdomain.com

# Docker环境中重新生成用户
docker exec coturn-server turnadmin -a -u webrtc -p SecurePassword123! -r yourdomain.com
```

#### 9.7 SSL证书问题
```bash
# 传统环境检查证书
sudo openssl x509 -in /etc/ssl/certs/turn_server_cert.pem -text -noout

# Docker环境检查证书
docker exec coturn-server openssl x509 -in /etc/ssl/certs/turn_server_cert.pem -text -noout

# 更新证书权限（传统环境）
sudo chown turnserver:turnserver /etc/ssl/private/turn_server_pkey.pem
sudo chmod 600 /etc/ssl/private/turn_server_pkey.pem

# Docker环境证书挂载
# 在docker-compose.yml中添加：
# volumes:
#   - /etc/letsencrypt:/etc/letsencrypt:ro
```

#### 9.8 性能问题

##### 传统环境优化
```bash
# 增加文件描述符限制
echo "turnserver soft nofile 65536" >> /etc/security/limits.conf
echo "turnserver hard nofile 65536" >> /etc/security/limits.conf

# 优化内核参数
echo "net.core.rmem_max = 134217728" >> /etc/sysctl.conf
echo "net.core.wmem_max = 134217728" >> /etc/sysctl.conf
sudo sysctl -p
```

##### Docker环境优化
```yaml
# 在docker-compose.yml中添加资源限制
services:
  coturn:
    # ... 其他配置 ...
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
    
    # 添加系统参数
    sysctls:
      - net.core.rmem_max=134217728
      - net.core.wmem_max=134217728
    
    # 增加文件描述符限制
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
```

### 通用调试工具

#### 9.9 连通性测试
```bash
# 使用turnutils工具测试（需要安装coturn-utils）
turnutils_peer -n -r your-server.com -u webrtc -w SecurePassword123!

# Docker环境测试
docker exec coturn-server turnutils_peer -n -r localhost -u webrtc -w SecurePassword123!

# 网络连通性测试
telnet your-server.com 3478
nc -u your-server.com 3478

# Docker容器网络测试
docker exec coturn-server telnet localhost 3478
```

#### 9.10 日志分析
```bash
# 分析错误日志
grep -i error /opt/coturn/logs/turnserver.log | tail -20

# 分析连接日志
grep "session" /opt/coturn/logs/turnserver.log | tail -20

# 实时监控特定错误
tail -f /opt/coturn/logs/turnserver.log | grep -i "authentication\|error\|failed"

# Docker环境日志分析
docker logs coturn-server 2>&1 | grep -i error
```

#### 9.11 性能分析
```bash
# 监控系统资源
htop
iotop
nethogs

# Docker容器资源监控
docker stats coturn-server

# 网络连接分析
ss -tuln | grep :3478
netstat -an | grep :3478 | wc -l

# 进程分析
ps aux | grep turnserver
pstree -p $(pgrep turnserver)
```

## 10. 安全建议

### 1. 访问控制
- 使用强密码
- 定期更换认证凭据
- 限制访问IP范围
- 启用SSL/TLS加密

### 2. 网络安全
- 配置防火墙规则
- 使用VPN访问
- 监控异常连接
- 定期安全审计

### 3. 服务器安全
- 定期更新系统
- 监控系统资源
- 备份配置文件
- 设置日志轮转

## 11. Docker快速部署脚本

### 11.1 一键部署脚本
```bash
#!/bin/bash
# docker-setup-turn-server.sh - Docker TURN服务器一键部署脚本

set -e

echo "=== Docker TURN服务器快速部署脚本 ==="
echo "此脚本将使用Docker快速部署coturn TURN服务器"
echo

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "Docker未安装，正在安装..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    echo "Docker安装完成，请重新登录后运行此脚本"
    exit 1
fi

# 检查Docker Compose是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "Docker Compose未安装，正在安装..."
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# 获取服务器信息
PUBLIC_IP=$(curl -s ifconfig.me || curl -s ipinfo.io/ip || echo "")
PRIVATE_IP=$(hostname -I | awk '{print $1}')

echo "检测到的公网IP: $PUBLIC_IP"
echo "检测到的内网IP: $PRIVATE_IP"
echo

# 用户输入配置信息
read -p "请输入您的域名（如 turn.example.com，默认使用IP）: " DOMAIN
DOMAIN=${DOMAIN:-$PUBLIC_IP}

read -p "请输入TURN用户名（默认: webrtc）: " TURN_USER
TURN_USER=${TURN_USER:-webrtc}

read -s -p "请输入TURN密码: " TURN_PASSWORD
echo
read -s -p "请再次确认密码: " TURN_PASSWORD_CONFIRM
echo

if [ "$TURN_PASSWORD" != "$TURN_PASSWORD_CONFIRM" ]; then
    echo "密码不匹配，退出安装"
    exit 1
fi

if [ -z "$TURN_PASSWORD" ]; then
    echo "密码不能为空，退出安装"
    exit 1
fi

# 创建部署目录
DEPLOY_DIR="/opt/coturn"
echo "创建部署目录: $DEPLOY_DIR"
sudo mkdir -p $DEPLOY_DIR/{config,logs,data,monitoring}
sudo chown -R $USER:$USER $DEPLOY_DIR

cd $DEPLOY_DIR

# 创建配置文件
echo "创建TURN服务器配置文件..."
cat > config/turnserver.conf << EOF
# Docker TURN服务器配置
listening-port=3478
tls-listening-port=5349
listening-ip=0.0.0.0
external-ip=$PUBLIC_IP

realm=$DOMAIN
server-name=$DOMAIN

lt-cred-mech
user=$TURN_USER:$TURN_PASSWORD

log-file=/var/log/turnserver.log
verbose

fingerprint
no-multicast-peers
no-cli
no-tlsv1
no-tlsv1_1

min-port=49152
max-port=65535

denied-peer-ip=10.0.0.0-10.255.255.255
denied-peer-ip=192.168.0.0-192.168.255.255
denied-peer-ip=172.16.0.0-172.31.255.255
denied-peer-ip=127.0.0.0-127.255.255.255
denied-peer-ip=0.0.0.0-0.255.255.255
denied-peer-ip=169.254.0.0-169.254.255.255

total-quota=100
bps-capacity=0
stale-nonce=600
EOF

# 创建Docker Compose文件
echo "创建Docker Compose配置..."
cat > docker-compose.yml << EOF
version: '3.8'

services:
  coturn:
    image: coturn/coturn:latest
    container_name: coturn-server
    restart: unless-stopped
    network_mode: host
    
    volumes:
      - ./config/turnserver.conf:/etc/coturn/turnserver.conf:ro
      - ./logs:/var/log
      - ./data:/var/lib/coturn
    
    environment:
      - TURN_USERNAME=$TURN_USER
      - TURN_PASSWORD=$TURN_PASSWORD
      - TURN_REALM=$DOMAIN
      - TURN_EXTERNAL_IP=$PUBLIC_IP
    
    command: [
      "-c", "/etc/coturn/turnserver.conf",
      "--log-file=/var/log/turnserver.log",
      "--pidfile=/var/run/turnserver.pid"
    ]
    
    healthcheck:
      test: ["CMD-SHELL", "turnutils_peer -n -r localhost -u $TURN_USER -w $TURN_PASSWORD || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # 可选：Watchtower自动更新
  watchtower:
    image: containrrr/watchtower:latest
    container_name: watchtower
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - WATCHTOWER_CLEANUP=true
      - WATCHTOWER_SCHEDULE=0 2 * * *
    command: --interval 86400
EOF

# 创建管理脚本
echo "创建管理脚本..."
cat > manage-turn.sh << 'EOF'
#!/bin/bash
# TURN服务器管理脚本

CONTAINER_NAME="coturn-server"

case "$1" in
    start)
        docker-compose up -d
        echo "TURN服务器已启动"
        ;;
    stop)
        docker-compose down
        echo "TURN服务器已停止"
        ;;
    restart)
        docker-compose restart
        echo "TURN服务器已重启"
        ;;
    status)
        docker-compose ps
        ;;
    logs)
        docker-compose logs -f coturn
        ;;
    update)
        docker-compose pull
        docker-compose up -d
        echo "TURN服务器已更新"
        ;;
    test)
        docker exec $CONTAINER_NAME turnutils_peer -n -r localhost -u ${TURN_USERNAME:-webrtc} -w ${TURN_PASSWORD:-password}
        ;;
    shell)
        docker exec -it $CONTAINER_NAME /bin/bash
        ;;
    stats)
        docker stats $CONTAINER_NAME
        ;;
    backup)
        tar -czf "coturn-backup-$(date +%Y%m%d-%H%M%S).tar.gz" config/ logs/ data/
        echo "备份完成"
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs|update|test|shell|stats|backup}"
        exit 1
        ;;
esac
EOF

chmod +x manage-turn.sh

# 创建监控脚本
echo "创建监控脚本..."
cat > monitor-turn.sh << 'EOF'
#!/bin/bash
# TURN服务器监控脚本

CONTAINER_NAME="coturn-server"

echo "=== TURN服务器监控报告 ==="
echo "时间: $(date)"
echo

# 容器状态
echo "容器状态:"
docker ps | grep $CONTAINER_NAME || echo "容器未运行"
echo

# 健康检查
echo "健康状态:"
HEALTH=$(docker inspect --format='{{.State.Health.Status}}' $CONTAINER_NAME 2>/dev/null)
echo "健康状态: ${HEALTH:-未配置}"
echo

# 资源使用
echo "资源使用:"
docker stats --no-stream $CONTAINER_NAME 2>/dev/null || echo "无法获取资源信息"
echo

# 端口监听
echo "端口监听:"
docker exec $CONTAINER_NAME netstat -tulpn 2>/dev/null | grep -E ":3478|:5349" || echo "端口信息获取失败"
echo

# 连接统计
echo "连接统计:"
CONNECTIONS=$(docker exec $CONTAINER_NAME netstat -an 2>/dev/null | grep :3478 | wc -l)
echo "当前连接数: $CONNECTIONS"
echo

# 最近日志
echo "最近日志 (最后10行):"
docker logs --tail 10 $CONTAINER_NAME 2>/dev/null || echo "日志获取失败"
EOF

chmod +x monitor-turn.sh

# 配置防火墙
echo "配置防火墙..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 3478/tcp
    sudo ufw allow 3478/udp
    sudo ufw allow 5349/tcp
    sudo ufw allow 5349/udp
    sudo ufw allow 49152:65535/udp
    echo "UFW防火墙配置完成"
elif command -v firewall-cmd &> /dev/null; then
    sudo firewall-cmd --permanent --add-port=3478/tcp
    sudo firewall-cmd --permanent --add-port=3478/udp
    sudo firewall-cmd --permanent --add-port=5349/tcp
    sudo firewall-cmd --permanent --add-port=5349/udp
    sudo firewall-cmd --permanent --add-port=49152-65535/udp
    sudo firewall-cmd --reload
    echo "firewalld防火墙配置完成"
fi

# 启动服务
echo "启动TURN服务器..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose ps

# 生成前端配置
echo "生成前端配置文件..."
cat > frontend-config.env << EOF
# WebRTC TURN服务器配置
# 将以下配置添加到您的前端项目的.env.local文件中

VITE_TURN_SERVER_URL=turn:$PUBLIC_IP:3478
VITE_TURNS_SERVER_URL=turns:$PUBLIC_IP:5349
VITE_TURN_USERNAME=$TURN_USER
VITE_TURN_CREDENTIAL=$TURN_PASSWORD
VITE_ENABLE_TURN=true
VITE_WEBRTC_DEBUG=false
EOF

# 创建定时任务
echo "配置定时任务..."
(crontab -l 2>/dev/null; echo "*/5 * * * * cd $DEPLOY_DIR && ./monitor-turn.sh >> logs/monitor.log 2>&1") | crontab -

echo
echo "=== 部署完成 ==="
echo "TURN服务器已成功部署并启动！"
echo
echo "部署信息："
echo "  部署目录: $DEPLOY_DIR"
echo "  域名/IP: $DOMAIN"
echo "  公网IP: $PUBLIC_IP"
echo "  用户名: $TURN_USER"
echo "  密码: [已设置]"
echo
echo "管理命令："
echo "  ./manage-turn.sh start    # 启动服务"
echo "  ./manage-turn.sh stop     # 停止服务"
echo "  ./manage-turn.sh status   # 查看状态"
echo "  ./manage-turn.sh logs     # 查看日志"
echo "  ./manage-turn.sh test     # 测试连通性"
echo "  ./monitor-turn.sh         # 监控报告"
echo
echo "前端配置文件: $DEPLOY_DIR/frontend-config.env"
echo
echo "在线测试工具："
echo "  https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/"
echo
echo "注意事项："
echo "1. 请确保防火墙端口已开放"
echo "2. 如需SSL支持，请手动配置证书"
echo "3. 建议定期备份配置文件"
echo "4. 监控服务器资源使用情况"
echo
echo "部署完成！"
EOF

# 使脚本可执行
chmod +x docker-setup-turn-server.sh
```

### 11.2 使用快速部署脚本
```bash
# 下载并运行部署脚本
wget https://raw.githubusercontent.com/your-repo/scripts/docker-setup-turn-server.sh
chmod +x docker-setup-turn-server.sh
sudo ./docker-setup-turn-server.sh
```

## 12. Docker vs 传统部署对比

| 特性 | Docker部署 | 传统部署 |
|------|------------|----------|
| **部署速度** | 快速，一键部署 | 需要手动配置多个步骤 |
| **环境隔离** | 完全隔离，无依赖冲突 | 可能存在系统依赖问题 |
| **可移植性** | 高，跨平台一致 | 依赖特定操作系统 |
| **资源使用** | 轻微额外开销 | 直接使用系统资源 |
| **更新维护** | 简单，容器化更新 | 需要手动更新配置 |
| **监控调试** | 容器化工具丰富 | 传统系统工具 |
| **备份恢复** | 配置文件+数据卷 | 完整系统备份 |
| **扩展性** | 易于水平扩展 | 需要额外配置 |

## 13. 推荐部署方案

### 13.1 开发环境
- **推荐**: Docker单容器部署
- **优势**: 快速启动，易于调试
- **命令**: `docker run -d --name coturn-dev --network host coturn/coturn`

### 13.2 测试环境
- **推荐**: Docker Compose部署
- **优势**: 完整的服务栈，包含监控
- **特点**: 自动化测试，CI/CD集成

### 13.3 生产环境
- **推荐**: Docker Compose + 外部监控
- **优势**: 高可用，完整监控，自动恢复
- **特点**: 负载均衡，SSL证书，安全加固

### 13.4 大规模部署
- **推荐**: Kubernetes + Helm
- **优势**: 自动扩缩容，服务发现，滚动更新
- **特点**: 企业级容器编排

通过以上Docker部署方案，可以更加便捷、可靠地在Linux服务器环境下部署TURN服务器，解决WebRTC在严格网络环境下的连接问题，确保音视频通话功能的稳定性和可靠性。Docker化部署提供了更好的可移植性、可维护性和扩展性。