# 校园小红书

> 基于 Spring Boot 3 + Vue 3 的校园内容分享社交平台，融合博客发布、社交互动、实时聊天与 AI 智能助手等功能。

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
  - [1. 克隆项目](#1-克隆项目)
  - [2. 数据库初始化](#2-数据库初始化)
  - [3. 后端启动](#3-后端启动)
  - [4. 前端启动](#4-前端启动)
- [Docker 部署](#docker-部署)
- [配置说明](#配置说明)
- [API 概览](#api-概览)
- [默认账号](#默认账号)

---

## 项目简介

**校园小红书**是一个面向校园场景的内容分享与社交平台。用户可以在平台上发布图文博客、浏览瀑布流内容、点赞收藏评论、关注其他用户，并通过实时聊天和 AI 智能助手进行互动。项目采用前后端分离架构，同时提供管理后台用于内容审核与平台运营。

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.5.0 | 核心框架 |
| Spring AI | 1.0.0 | AI 模型集成（Ollama） |
| MyBatis-Plus | 3.5.9 | ORM 持久层框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | - | 缓存 & AI 对话记忆存储 |
| WebSocket | - | 实时消息推送 & 聊天 |
| JWT (auth0) | 4.4.0 | 身份认证 |
| 阿里云 OSS | 3.10.2 | 文件/图片存储 |
| Hutool | 5.8.21 | Java 工具类库 |
| FastJSON | 2.0.41 | JSON 序列化 |
| Lombok | 1.18.38 | 代码简化 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.13 | 前端框架 |
| Vue Router | 4.5.0 | 路由管理 |
| Vite | 6.2.4 | 构建工具 |
| Element Plus | 2.9.9 | UI 组件库 |
| Axios | 1.9.0 | HTTP 请求 |
| ECharts | 5.6.0 | 数据可视化图表 |
| WangEditor | 5.1.23 | 富文本编辑器 |
| highlight.js | 11.11.1 | 代码高亮 |
| marked | 17.0.1 | Markdown 渲染 |
| Sass | 1.87.0 | CSS 预处理器 |

---

## 功能模块

### 前台（用户端）

- **用户注册 / 登录 / 密码找回** — 支持安全问答找回密码
- **内容发现** — 瀑布流展示博客内容，支持分类筛选和搜索
- **博客发布** — 富文本编辑器撰写图文博客
- **社交互动** — 点赞、收藏、评论、关注
- **消息通知** — 系统消息与互动提醒
- **实时聊天** — 基于 WebSocket 的用户间即时通讯
- **AI 智能助手** — 基于 Ollama 大模型的对话助手，支持上下文记忆
- **个人中心** — 个人资料编辑、密码修改、我的博客/收藏/点赞管理
- **用户主页** — 查看其他用户发布的内容
- **响应式布局** — 适配桌面端与移动端

### 后台（管理端）

- **数据仪表盘** — 平台数据统计与可视化
- **用户管理** — 用户信息查看与管理
- **管理员管理** — 管理员账号管理
- **博客管理** — 博客内容审核与管理
- **分类管理** — 博客分类的增删改查
- **评论管理** — 评论内容审核与管理
- **收藏 / 点赞 / 关注管理** — 互动数据管理
- **消息管理** — 系统消息发送与管理
- **聊天信息管理** — 聊天记录查看
- **敏感词管理** — 敏感词过滤配置
- **AI 助手管理** — AI 助手参数配置（模型、提示词等）

---

## 项目结构

```
校园小红书/
├── sql/                          # 数据库脚本
│   ├── base.sql                  # 基础表（用户、管理员）
│   └── blog.sql                  # 业务表（博客、评论、收藏等）
├── src/main/java/com/example/springboot/
│   ├── common/                   # 通用类（常量、统一响应、WebSocket）
│   ├── config/                   # 配置类（跨域、拦截器、MyBatis、Redis、AI等）
│   ├── controller/               # 控制器层（REST API）
│   ├── entity/                   # 实体类
│   ├── exception/                # 全局异常处理
│   ├── mapper/                   # MyBatis Mapper 接口
│   ├── properties/               # 配置属性类
│   ├── service/                  # 业务逻辑层
│   │   └── impl/                 # 服务实现类
│   └── utils/                    # 工具类（JWT、OSS等）
├── src/main/resources/
│   └── application.yml           # 后端应用配置
├── vue/                          # 前端项目
│   ├── config/                   # 前端配置文件
│   ├── src/
│   │   ├── router/               # 路由配置
│   │   ├── utils/                # 请求工具（Axios 封装）
│   │   ├── views/
│   │   │   ├── front/            # 前台页面
│   │   │   ├── back/             # 后台管理页面
│   │   │   ├── Front.vue         # 前台布局
│   │   │   ├── Back.vue          # 后台布局
│   │   │   ├── Login.vue         # 登录页
│   │   │   ├── Register.vue      # 注册页
│   │   │   └── PasswordReset.vue # 密码找回页
│   │   └── main.js               # 入口文件
│   └── vite.config.js            # Vite 配置
├── .env                          # 环境变量配置
├── Dockerfile                    # 后端 Docker 镜像
├── vue/Dockerfile                # 前端 Docker 镜像
└── pom.xml                       # Maven 依赖管理
```

---

## 环境要求

| 环境 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Ollama（可选） | 最新版 |

---

## 快速开始

### 1. 克隆项目

```bash
# GitHub
git clone https://github.com/1123074811/Campus-Xiaohongshu.git

# 或 Gitee（国内推荐）
git clone https://gitee.com/ou-jincong/blog-forum.git

cd Campus-Xiaohongshu
```

### 2. 数据库初始化

创建 MySQL 数据库 `blog`，然后依次执行 SQL 脚本：

```sql
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog;

-- 依次导入以下脚本
SOURCE sql/base.sql;   -- 基础表：管理员、用户
SOURCE sql/blog.sql;   -- 业务表：博客、分类、评论、收藏、点赞、关注、消息、聊天、敏感词、AI助手等
```

### 3. 后端启动

#### 配置环境变量

编辑项目根目录的 `.env` 文件，根据实际环境修改配置：

```properties
# 数据库
DB_URL=jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=你的数据库密码

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=你的Redis密码
REDIS_DATABASE=0

# Ollama AI（可选）
OLLAMA_BASE_URL=http://localhost:11434

# 阿里云 OSS（可选，用于图片上传）
ALIOSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
ALIOSS_ACCESS_KEY_ID=你的AccessKey
ALIOSS_ACCESS_KEY_SECRET=你的AccessSecret
ALIOSS_BUCKET_NAME=你的BucketName
```

#### 编译运行

```bash
# 编译打包
mvn clean package -DskipTests

# 运行
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

后端服务默认运行在 `http://localhost:9090`。

### 4. 前端启动

```bash
cd vue

# 安装依赖
npm install

# 开发模式启动
npm run dev
```

前端默认运行在 `http://localhost:5173`（Vite 默认端口），请确保 `vue/config/config.default.js` 中的后端地址配置正确。

#### 前端生产构建

```bash
npm run build
```

构建产物输出至 `vue/dist/` 目录。

---

## Docker 部署

### 后端镜像

```bash
# 先执行 Maven 打包
mvn clean package -DskipTests

# 在项目根目录构建镜像
docker build -t campus-blog-backend .

# 运行容器
docker run -d -p 9090:9090 --name campus-blog-backend campus-blog-backend
```

### 前端镜像

```bash
# 先执行前端构建
cd vue && npm run build

# 构建镜像
docker build -t campus-blog-frontend .

# 运行容器
docker run -d -p 80:80 --name campus-blog-frontend campus-blog-frontend
```

---

## 配置说明

### 后端端口

在 `src/main/resources/application.yml` 中修改：

```yaml
server:
  port: 9090
```

### 前端接口地址

在 `vue/config/config.default.js` 中修改：

```javascript
const ip = '127.0.0.1'
const port = '9090'
const serverHost = 'http://' + ip + ':' + port
```

### AI 助手

项目集成 Spring AI + Ollama 实现智能对话功能。需确保：

1. 安装并启动 [Ollama](https://ollama.ai)
2. 拉取模型：`ollama pull llama3`（或其他模型）
3. 在后台管理 → AI 助手管理中配置模型名称和系统提示词
4. AI 对话记忆使用 Redis 存储，需确保 Redis 正常运行

### 文件上传（阿里云 OSS）

项目使用阿里云 OSS 存储图片等文件。若不使用 OSS，文件将存储在本地服务器。配置方式见 `.env` 文件中的 OSS 相关参数。

---

## API 概览

后端所有接口统一前缀为 `http://localhost:9090`，主要模块如下：

| 模块 | 控制器 | 说明 |
|------|--------|------|
| 用户 | `UserController` | 注册、登录、用户信息 CRUD |
| 管理员 | `AdminController` | 管理员登录与管理 |
| 博客 | `BlogController` | 博客发布、编辑、删除、列表 |
| 分类 | `TypeController` | 博客分类管理 |
| 评论 | `CommentController` | 评论发布与管理 |
| 点赞 | `LikeController` | 点赞/取消点赞 |
| 收藏 | `CollectController` | 收藏/取消收藏 |
| 关注 | `FollowController` | 关注/取消关注 |
| 消息 | `MessageController` | 系统消息通知 |
| 聊天 | `ChatController` | 聊天消息记录 |
| AI 助手 | `AiAssistantController` | AI 对话接口 |
| AI 管理 | `AdminAiAssistantController` | AI 助手配置管理 |
| 仪表盘 | `AdminDashboardController` | 后台统计数据 |
| 密码重置 | `PasswordResetController` | 密码找回 |
| 敏感词 | `WordController` | 敏感词过滤管理 |
| 文件 | `WebController` | 文件上传与下载 |

---

## 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin` |
| 普通用户 | `111` | `111` |

> ⚠️ **安全提示**：请在部署到生产环境前及时修改默认账号密码。

---

## 许可证

本项目仅供学习与参考使用。
