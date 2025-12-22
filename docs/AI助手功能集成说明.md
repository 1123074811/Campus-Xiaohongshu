# AI助手功能集成说明

## 概述

本项目已成功集成AI助手功能，基于Spring AI框架和Ollama模型，为用户提供智能对话服务。

## 功能特性

### 1. 智能对话
- 基于Ollama deepseek-r1:7b模型
- 支持流式回复，实时显示AI响应
- 智能上下文记忆，支持多轮对话
- 个性化AI助手"哈基聪"

### 2. 会话管理
- 多会话支持，用户可创建多个对话
- 会话标题自动生成和手动编辑
- 会话历史记录持久化存储
- 会话删除和管理功能

### 3. 用户体验
- 响应式设计，支持移动端
- 实时打字效果显示
- 快速问题模板
- 消息时间显示和格式化

## 技术架构

### 后端技术栈
- **Spring Boot 3.5.0** - 主框架
- **Spring AI 1.0.0** - AI集成框架
- **Ollama** - 本地AI模型服务
- **Redis** - 聊天记忆存储
- **MySQL** - 数据持久化
- **MyBatis Plus** - ORM框架

### 前端技术栈
- **Vue 3** - 前端框架
- **Element Plus** - UI组件库
- **Fetch API** - 流式数据处理

## 部署配置

### 1. 环境要求

#### Ollama安装
```bash
# 安装Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 下载deepseek-r1模型
ollama pull deepseek-r1:7b

# 启动Ollama服务
ollama serve
```

#### Redis安装
```bash
# 使用Docker安装Redis
docker run -d --name redis -p 6379:6379 redis:latest

# 或使用本地安装
# Windows: 下载Redis for Windows
# Linux: sudo apt-get install redis-server
# macOS: brew install redis
```

### 2. 数据库配置

执行数据库初始化脚本：
```sql
-- 在现有数据库中执行
source springboot/sql/init_ai_assistant.sql
```

### 3. 应用配置

后端配置文件 `application.yaml` 已更新：
```yaml
spring:
  # Redis配置
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
  # Spring AI配置
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: deepseek-r1:7b
```

### 4. 依赖更新

后端 `pom.xml` 已添加必要依赖：
```xml
<!-- Spring AI Ollama -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>

<!-- Redis for chat memory -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 项目结构

### 后端新增文件
```
springboot/src/main/java/com/example/springboot/
├── entity/
│   ├── AiChatSession.java          # AI聊天会话实体
│   ├── AiChatMessage.java          # AI聊天消息实体
│   └── AiAssistantConfig.java      # AI助手配置实体
├── mapper/
│   ├── AiChatSessionMapper.java    # 会话数据访问层
│   ├── AiChatMessageMapper.java    # 消息数据访问层
│   └── AiAssistantConfigMapper.java # 配置数据访问层
├── service/
│   ├── IAiChatService.java         # AI聊天服务接口
│   └── impl/
│       └── AiChatServiceImpl.java  # AI聊天服务实现
├── controller/
│   └── AiAssistantController.java  # AI助手控制器
└── config/
    ├── AiConfiguration.java        # AI配置类
    └── RedisChatMemoryRepository.java # Redis聊天记忆存储
```

### 前端新增文件
```
vue/src/
├── views/front/
│   └── AiAssistant.vue            # AI助手页面
└── router/
    └── index.js                   # 路由配置(已更新)
```

### 数据库表结构
```
├── ai_chat_session               # AI聊天会话表
├── ai_chat_message              # AI聊天消息表
└── ai_assistant_config          # AI助手配置表
```

## API接口

### 会话管理
- `POST /ai/session` - 创建新会话
- `GET /ai/sessions` - 获取用户会话列表
- `PUT /ai/session/{sessionId}/title` - 更新会话标题
- `DELETE /ai/session/{sessionId}` - 删除会话

### 消息管理
- `GET /ai/session/{sessionId}/messages` - 获取会话消息
- `POST /ai/chat` - 发送消息并获取AI回复(流式)

### 其他
- `GET /ai/welcome` - 获取欢迎消息

## 使用说明

### 1. 访问AI助手
- 登录系统后，点击左侧导航栏的"AI助手"
- 或直接访问 `/front/ai-assistant`

### 2. 开始对话
- 点击"新对话"创建会话
- 在输入框中输入问题
- 按Enter发送消息
- 观察AI实时回复

### 3. 会话管理
- 点击会话标题旁的编辑按钮修改标题
- 点击删除按钮删除不需要的会话
- 系统自动保存聊天记录

## 配置说明

### AI模型配置
- 默认使用 `deepseek-r1:7b` 模型
- 可在 `application.yaml` 中修改模型名称
- 支持Ollama支持的所有模型

### 聊天记忆配置
- 默认保存最近20条消息作为上下文
- Redis存储，24小时过期
- 可在 `AiConfiguration.java` 中调整

### 系统提示词
- 默认设置AI助手名称为"哈基聪"
- 可在数据库 `ai_assistant_config` 表中修改
- 支持个性化定制

## 故障排除

### 1. AI无法回复
- 检查Ollama服务是否启动：`ollama serve`
- 确认模型已下载：`ollama list`
- 检查网络连接到 `localhost:11434`

### 2. 聊天记录丢失
- 检查Redis服务状态
- 确认Redis连接配置正确
- 查看应用日志中的Redis连接错误

### 3. 流式回复异常
- 检查浏览器是否支持Fetch API
- 确认网络连接稳定
- 查看控制台错误信息

## 扩展功能

### 1. 多模型支持
可以扩展支持多个AI模型：
```yaml
spring:
  ai:
    ollama:
      models:
        - name: deepseek-r1:7b
        - name: llama2:7b
        - name: codellama:7b
```

### 2. 文件上传
可以扩展支持文档分析：
- PDF文档解析
- 图片识别
- 代码分析

### 3. 语音交互
可以集成语音功能：
- 语音输入
- 语音播放
- 实时语音对话

## 性能优化

### 1. 缓存策略
- Redis缓存热门对话
- 本地缓存配置信息
- CDN加速静态资源

### 2. 并发处理
- 异步处理AI请求
- 连接池优化
- 限流保护

### 3. 监控告警
- AI服务健康检查
- 响应时间监控
- 错误率统计

## 安全考虑

### 1. 用户隔离
- 会话级别权限控制
- 用户数据隔离
- 敏感信息过滤

### 2. 内容审核
- 输入内容过滤
- 输出内容检查
- 违规内容拦截

### 3. 访问控制
- 登录用户验证
- API访问限制
- 防止滥用机制

---

## 总结

AI助手功能已成功集成到现有项目中，提供了完整的智能对话体验。通过合理的架构设计和配置，系统具备良好的扩展性和稳定性。用户可以享受流畅的AI对话服务，同时管理员可以灵活配置和监控系统运行状态。