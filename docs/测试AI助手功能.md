# AI助手功能测试指南

## 🚀 快速测试步骤

### 1. 启动必要服务

#### 启动Ollama服务
```bash
# 确保Ollama已安装并启动
ollama serve

# 在另一个终端下载模型（如果还没有）
ollama pull deepseek-r1:7b
```

#### 启动Redis服务
```bash
# 使用Docker启动Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 或者启动本地Redis服务
redis-server
```

### 2. 初始化数据库
```sql
-- 在MySQL中执行
USE blog;
source springboot/sql/init_ai_assistant.sql;
```

### 3. 启动应用
```bash
# 启动后端
cd springboot
mvn spring-boot:run

# 启动前端
cd vue
npm run dev
```

### 4. 测试功能

#### 访问AI助手页面
1. 登录系统
2. 点击左侧导航栏的"AI助手"图标
3. 点击"新对话"按钮

#### 测试基本对话
1. 输入："你好，请介绍一下自己"
2. 观察AI是否能正常回复
3. 继续对话测试上下文记忆

#### 测试会话管理
1. 创建多个会话
2. 测试会话标题编辑
3. 测试会话删除功能

## 🔧 故障排除

### 问题1：AI无法回复
**症状**：发送消息后没有回复或报错

**解决方案**：
```bash
# 检查Ollama服务状态
curl http://localhost:11434/api/tags

# 检查模型是否存在
ollama list

# 重启Ollama服务
ollama serve
```

### 问题2：聊天记录不保存
**症状**：刷新页面后聊天记录消失

**解决方案**：
```bash
# 检查Redis连接
redis-cli ping

# 检查数据库表是否创建
mysql -u root -p -e "USE blog; SHOW TABLES LIKE 'ai_%';"
```

### 问题3：流式回复异常
**症状**：消息发送后卡住或显示异常

**解决方案**：
1. 检查浏览器控制台错误
2. 确认网络连接稳定
3. 检查后端日志

## 📊 API测试

### 使用curl测试API
```bash
# 获取token（先登录获取）
TOKEN="your_token_here"

# 创建会话
curl -X POST "http://localhost:9090/ai/session" \
  -H "Content-Type: application/json" \
  -H "token: $TOKEN" \
  -d '{"title": "测试会话"}'

# 获取会话列表
curl -X GET "http://localhost:9090/ai/sessions" \
  -H "token: $TOKEN"

# 发送消息（流式响应）
curl -X POST "http://localhost:9090/ai/chat?sessionId=SESSION_ID&message=你好" \
  -H "token: $TOKEN"
```

## 🎯 功能验证清单

- [ ] Ollama服务正常运行
- [ ] Redis服务正常运行
- [ ] 数据库表创建成功
- [ ] 后端应用启动无错误
- [ ] 前端应用启动无错误
- [ ] 能够访问AI助手页面
- [ ] 能够创建新会话
- [ ] 能够发送消息并收到AI回复
- [ ] 流式回复显示正常
- [ ] 会话标题可以编辑
- [ ] 会话可以删除
- [ ] 聊天记录持久化保存
- [ ] 多轮对话上下文正常

## 📝 测试用例

### 基础对话测试
1. **问候测试**
   - 输入："你好"
   - 预期：AI友好回复并介绍自己

2. **功能询问**
   - 输入："你能做什么？"
   - 预期：AI介绍自己的功能

3. **上下文测试**
   - 输入："我叫张三"
   - 输入："我叫什么名字？"
   - 预期：AI能记住并回答"张三"

### 会话管理测试
1. **多会话测试**
   - 创建3个不同的会话
   - 在每个会话中发送不同的消息
   - 切换会话验证消息隔离

2. **会话持久化测试**
   - 发送消息后刷新页面
   - 验证消息是否还在

## 🔍 日志检查

### 后端日志关键信息
```
# 正常启动日志
Started SpringbootApplication
Ollama chat model initialized
Redis connection established

# AI对话日志
AI chat request: sessionId=xxx, message=xxx
AI response completed: xxx characters
```

### 前端控制台检查
```javascript
// 正常情况下应该看到
console.log('AI助手页面加载完成')
console.log('会话列表加载成功')
console.log('消息发送成功')
```

---

## 📞 技术支持

如果遇到问题，请检查：
1. 所有服务是否正常启动
2. 网络连接是否正常
3. 配置文件是否正确
4. 日志中是否有错误信息

按照此测试指南，你应该能够成功验证AI助手功能的完整性。