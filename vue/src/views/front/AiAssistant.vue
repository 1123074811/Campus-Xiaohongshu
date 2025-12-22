<template>
  <div class="ai-assistant-container">
    <!-- 侧边栏 - 会话列表 -->
    <div class="sessions-panel">
      <div class="panel-header">
        <div class="header-left">
          <h3>AI助手</h3>
        </div>
        <el-button 
          type="primary" 
          size="small" 
          @click="createNewSession"
          :icon="Plus"
        >
          新对话
        </el-button>
      </div>

      <div class="sessions-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ 'active': currentSession?.sessionId === session.sessionId }"
          @click="selectSession(session)"
        >
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatTime(session.updateTime) }}</div>
          </div>
          <div class="session-actions">
            <el-button
              type="text"
              size="small"
              @click.stop="editSessionTitle(session)"
              :icon="Edit"
            />
            <el-button
              type="text"
              size="small"
              @click.stop="deleteSession(session)"
              :icon="Delete"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="chat-panel">
      <template v-if="currentSession">
        <div class="chat-header">
          <div class="ai-info">
            <div class="ai-avatar">
              <el-icon :size="24"><Service /></el-icon>
            </div>
            <div>
              <span class="ai-name">哈基聪</span>
              <span class="ai-status">智能助手</span>
            </div>
          </div>
          
          <!-- 思考内容显示控制 -->
          <div class="thinking-control">
            <el-switch
              v-model="showThinking"
              size="small"
              active-text="显示思考"
              inactive-text="隐藏思考"
              :active-value="true"
              :inactive-value="false"
            />
          </div>
        </div>

        <div class="messages-container" ref="messagesContainer">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="ai-avatar-large">
              <el-icon :size="48"><Service /></el-icon>
            </div>
            <h3>你好！我是哈基聪</h3>
            <p>我是你的智能助手，有什么可以帮助你的吗？</p>
            <div class="quick-questions">
              <el-button
                v-for="question in quickQuestions"
                :key="question"
                type="default"
                size="small"
                @click="sendQuickQuestion(question)"
                class="quick-question-btn"
              >
                {{ question }}
              </el-button>
            </div>
          </div>

          <!-- 聊天消息 -->
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-wrapper"
            :class="{ 'message-user': message.messageType === 'USER' }"
          >
            <div class="message-avatar" v-if="message.messageType === 'ASSISTANT'">
              <el-icon :size="20"><Service /></el-icon>
            </div>

            <div class="message-content">
              <div class="message-bubble" :class="{
                'bubble-user': message.messageType === 'USER',
                'bubble-assistant': message.messageType === 'ASSISTANT'
              }">
                <div v-if="message.messageType === 'ASSISTANT'" class="message-text" v-html="formatMessage(message.content)"></div>
                <div v-else class="message-text">{{ message.content }}</div>
              </div>
              <div class="message-time">{{ formatTime(message.createTime) }}</div>
            </div>

            <div class="message-avatar" v-if="message.messageType === 'USER'">
              <img :src="account.avatarUrl" alt="用户头像">
            </div>
          </div>

          <!-- AI正在输入指示器 -->
          <div v-if="isAiTyping" class="message-wrapper">
            <div class="message-avatar">
              <el-icon :size="20"><Service /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble bubble-assistant">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 流式回复显示 -->
          <div v-if="streamingMessage" class="message-wrapper">
            <div class="message-avatar">
              <el-icon :size="20"><Service /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-bubble bubble-assistant">
                <div class="message-text" v-html="formatMessage(streamingMessage)"></div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-container">
          <div class="message-editor">
            <div class="textarea-wrapper">
              <el-input
                type="textarea"
                v-model="inputText"
                :rows="1"
                placeholder="请输入你的问题..."
                resize="none"
                @keydown="handleKeydown"
                class="message-textarea"
                :disabled="isAiTyping"
              />
            </div>
          </div>

          <div class="send-actions">
            <span class="hint">
              <el-icon size="14"><Position/></el-icon>
              按 Enter 发送，Shift + Enter 换行
            </span>
            <el-button
              type="primary"
              @click="sendMessage"
              :disabled="!inputText.trim() || isAiTyping"
              :loading="isAiTyping"
            >
              {{ isAiTyping ? '思考中...' : '发送消息' }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="no-session-selected" v-else>
        <el-icon :size="64"><Service /></el-icon>
        <h3>开始与AI助手对话</h3>
        <p>点击左侧"新对话"按钮开始聊天</p>
      </div>
    </div>

    <!-- 编辑会话标题对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑会话标题"
      width="400px"
    >
      <el-input
        v-model="editingTitle"
        placeholder="请输入会话标题"
        maxlength="50"
        show-word-limit
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="saveSessionTitle">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Service, Plus, Edit, Delete, Position } from '@element-plus/icons-vue'

const router = useRouter()

// 响应式数据
const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const inputText = ref('')
const isAiTyping = ref(false)
const streamingMessage = ref('')
const messagesContainer = ref(null)
const showThinking = ref(true) // 控制是否显示思考内容，默认显示

// 编辑会话标题相关
const showEditDialog = ref(false)
const editingSession = ref(null)
const editingTitle = ref('')

// 快速问题
const quickQuestions = ref([
  '你好，请介绍一下自己',
  '今天天气怎么样？',
  '帮我写一个简单的代码',
  '推荐一些学习资源'
])

// 计算属性
const uploadHeaders = computed(() => {
  const currentAccount = localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : null
  return currentAccount && currentAccount.token ? {
    'token': currentAccount.token
  } : {}
})

// 生命周期
onMounted(() => {
  loadSessions()
  // 从本地存储加载思考内容显示设置
  const savedShowThinking = localStorage.getItem('showThinking')
  if (savedShowThinking !== null) {
    showThinking.value = JSON.parse(savedShowThinking)
  }
})

// 监听思考内容显示状态变化
watch(showThinking, (newValue) => {
  // 保存到本地存储
  localStorage.setItem('showThinking', JSON.stringify(newValue))
  // 强制重新渲染消息以应用新的显示设置
  nextTick(() => {
    // 触发消息重新格式化
    messages.value = [...messages.value]
  })
})

// 方法
const loadSessions = async () => {
  try {
    const res = await request.get('/ai/sessions', {
      headers: uploadHeaders.value
    })
    sessions.value = res.data || []
    
    // 如果有会话，默认选择第一个
    if (sessions.value.length > 0 && !currentSession.value) {
      selectSession(sessions.value[0])
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    ElMessage.error('加载会话列表失败')
  }
}

const createNewSession = async () => {
  try {
    const res = await request.post('/ai/session', {
      title: '新对话'
    }, {
      headers: uploadHeaders.value
    })
    
    const newSession = res.data
    sessions.value.unshift(newSession)
    selectSession(newSession)
    
    ElMessage.success('创建新对话成功')
  } catch (error) {
    console.error('创建会话失败:', error)
    ElMessage.error('创建会话失败')
  }
}

const selectSession = async (session) => {
  currentSession.value = session
  await loadMessages(session.sessionId)
}

const loadMessages = async (sessionId) => {
  try {
    const res = await request.get(`/ai/session/${sessionId}/messages`, {
      headers: uploadHeaders.value
    })
    messages.value = res.data || []
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!inputText.value.trim() || !currentSession.value) return
  
  const userMessage = inputText.value.trim()
  inputText.value = ''
  
  // 添加用户消息到界面
  const userMsg = {
    messageType: 'USER',
    content: userMessage,
    createTime: new Date().toISOString()
  }
  messages.value.push(userMsg)
  scrollToBottom()
  
  // 开始AI回复
  isAiTyping.value = true
  streamingMessage.value = ''
  
  try {
    // 使用fetch进行流式请求
    const response = await fetch(`${request.defaults.baseURL}/ai/chat?sessionId=${currentSession.value.sessionId}&message=${encodeURIComponent(userMessage)}`, {
      method: 'POST',
      headers: {
        'token': account.value.token,
        'Content-Type': 'application/json'
      }
    })
    
    if (!response.ok) {
      throw new Error('网络请求失败')
    }
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      streamingMessage.value += chunk
      scrollToBottom()
    }
    
    // 流式回复完成，保存AI消息
    const aiMsg = {
      messageType: 'ASSISTANT',
      content: streamingMessage.value,
      createTime: new Date().toISOString()
    }
    messages.value.push(aiMsg)
    
    // 清理流式消息
    streamingMessage.value = ''
    
    // 更新会话标题（如果是第一条消息）
    if (messages.value.filter(m => m.messageType === 'USER').length === 1) {
      updateSessionTitle(currentSession.value.sessionId, userMessage.substring(0, 20))
    }
    
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败，请重试')
  } finally {
    isAiTyping.value = false
    scrollToBottom()
  }
}

const sendQuickQuestion = (question) => {
  inputText.value = question
  sendMessage()
}

const editSessionTitle = (session) => {
  editingSession.value = session
  editingTitle.value = session.title
  showEditDialog.value = true
}

const saveSessionTitle = async () => {
  if (!editingTitle.value.trim()) {
    ElMessage.warning('请输入会话标题')
    return
  }
  
  try {
    await request.put(`/ai/session/${editingSession.value.sessionId}/title`, {
      title: editingTitle.value.trim()
    }, {
      headers: uploadHeaders.value
    })
    
    // 更新本地数据
    const session = sessions.value.find(s => s.sessionId === editingSession.value.sessionId)
    if (session) {
      session.title = editingTitle.value.trim()
    }
    
    showEditDialog.value = false
    ElMessage.success('会话标题更新成功')
  } catch (error) {
    console.error('更新会话标题失败:', error)
    ElMessage.error('更新会话标题失败')
  }
}

const updateSessionTitle = async (sessionId, title) => {
  try {
    await request.put(`/ai/session/${sessionId}/title`, {
      title: title
    }, {
      headers: uploadHeaders.value
    })
    
    // 更新本地数据
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session) {
      session.title = title
    }
  } catch (error) {
    console.error('自动更新会话标题失败:', error)
  }
}

const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个会话吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await request.delete(`/ai/session/${session.sessionId}`, {
      headers: uploadHeaders.value
    })
    
    // 从列表中移除
    const index = sessions.value.findIndex(s => s.sessionId === session.sessionId)
    if (index > -1) {
      sessions.value.splice(index, 1)
    }
    
    // 如果删除的是当前会话，清空当前会话
    if (currentSession.value?.sessionId === session.sessionId) {
      currentSession.value = null
      messages.value = []
      
      // 选择下一个会话
      if (sessions.value.length > 0) {
        selectSession(sessions.value[0])
      }
    }
    
    ElMessage.success('会话删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error('删除会话失败')
    }
  }
}

const formatMessage = (content) => {
  if (!content) return ''
  
  // 处理思考内容 - 将所有思考块合并为一个
  let processedContent = content
  
  // 检查是否包含思考标签
  const hasThink = content.includes('<think>') && content.includes('</think>')
  const hasThinking = content.includes('<thinking>') && content.includes('</thinking>')
  
  if (hasThink || hasThinking) {
    // 提取所有思考内容
    const thinkContents = []
    
    // 提取 <think> 标签内容
    const thinkMatches = content.matchAll(/<think>([\s\S]*?)<\/think>/g)
    for (const match of thinkMatches) {
      thinkContents.push(match[1])
    }
    
    // 提取 <thinking> 标签内容
    const thinkingMatches = content.matchAll(/<thinking>([\s\S]*?)<\/thinking>/g)
    for (const match of thinkingMatches) {
      thinkContents.push(match[1])
    }
    
    // 移除所有思考标签
    processedContent = processedContent
      .replace(/<think>[\s\S]*?<\/think>/g, '')
      .replace(/<thinking>[\s\S]*?<\/thinking>/g, '')
      .trim() // 移除多余的空白
    
    // 如果需要显示思考内容，在开头添加合并后的思考块
    if (showThinking.value && thinkContents.length > 0) {
      const mergedThinking = thinkContents.join('\n\n').trim()
      const thinkingBlock = `<div class="thinking-content"><div class="thinking-header"><i class="thinking-icon">🤔</i> AI思考过程</div><div class="thinking-text">${mergedThinking}</div></div>`
      processedContent = thinkingBlock + processedContent
    }
  }
  
  // 简单的markdown格式化
  return processedContent
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  
  try {
    const time = new Date(timeStr)
    
    // 检查日期是否有效
    if (isNaN(time.getTime())) {
      return ''
    }
    
    const now = new Date()
    const diff = now - time
    
    if (diff < 60000) { // 1分钟内
      return '刚刚'
    } else if (diff < 3600000) { // 1小时内
      return `${Math.floor(diff / 60000)}分钟前`
    } else if (diff < 86400000) { // 1天内
      return `${Math.floor(diff / 3600000)}小时前`
    } else {
      // 使用与聊天页面相同的格式
      return time.toLocaleString('zh-cn')
    }
  } catch (error) {
    console.error('时间格式化错误:', error)
    return ''
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}
</script>
<style scoped>
.ai-assistant-container {
  display: flex;
  height: calc(90vh - 90px);
  min-height: 600px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  margin: 20px auto;
  max-width: 1200px;
}

.sessions-panel {
  width: 280px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
}

.panel-header {
  height: 60px;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 6px;
  position: relative;
}

.session-item:hover {
  background-color: #f5f5f5;
}

.session-item.active {
  background-color: #f0f7ff;
  border: 1px solid #91d5ff;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-time {
  font-size: 12px;
  color: #999;
}

.session-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  height: 60px;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
}

.thinking-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-control :deep(.el-switch__label) {
  font-size: 12px;
  color: #666;
}

.ai-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.ai-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  display: block;
}

.ai-status {
  font-size: 12px;
  color: #52c41a;
  background-color: #f6ffed;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #b7eb8f;
  display: block;
  margin-top: 2px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9fafc;
  background-image: 
    linear-gradient(rgba(240, 240, 240, 0.5) 1px, transparent 1px),
    linear-gradient(90deg, rgba(240, 240, 240, 0.5) 1px, transparent 1px);
  background-size: 20px 20px;
}

.welcome-message {
  text-align: center;
  padding: 40px 20px;
  color: #666;
}

.ai-avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin: 0 auto 20px;
}

.welcome-message h3 {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.welcome-message p {
  margin: 0 0 30px;
  font-size: 16px;
  color: #666;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.quick-question-btn {
  border-radius: 20px;
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  background-color: #fff;
  color: #666;
  transition: all 0.3s ease;
}

.quick-question-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
  background-color: #f0f7ff;
}

.message-wrapper {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
}

.message-wrapper.message-user {
  justify-content: flex-end;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-wrapper.message-user .message-avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-wrapper:not(.message-user) .message-content {
  align-items: flex-start;
}

.message-wrapper.message-user .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
  max-width: 100%;
}

.message-bubble.bubble-assistant {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 4px;
  border: 1px solid #f0f0f0;
}

.message-bubble.bubble-user {
  background-color: #1890ff;
  color: #fff;
  border-top-right-radius: 4px;
}

.message-text {
  white-space: pre-wrap;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(em) {
  font-style: italic;
}

.message-text :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.bubble-user .message-text :deep(code) {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 思考内容样式 */
.message-text :deep(.thinking-content) {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.message-text :deep(.thinking-header) {
  background-color: #e3f2fd;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: #1976d2;
  border-bottom: 1px solid #bbdefb;
  display: flex;
  align-items: center;
  gap: 6px;
}

.message-text :deep(.thinking-icon) {
  font-size: 14px;
}

.message-text :deep(.thinking-text) {
  padding: 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #555;
  background-color: #fff;
  font-style: italic;
  border-left: 3px solid #2196f3;
  margin: 0;
}

.message-text :deep(.thinking-text br) {
  margin-bottom: 4px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 4px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #1890ff;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-container {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;
}

.message-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.textarea-wrapper {
  position: relative;
}

.message-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: #e8e8e8;
  padding: 12px;
  transition: all 0.3s;
  font-size: 14px;
  line-height: 1.6;
}

.message-textarea :deep(.el-textarea__inner):focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.send-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-session-selected {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  padding: 20px;
  text-align: center;
  background-color: #fafafa;
}

.no-session-selected .el-icon {
  margin-bottom: 20px;
  color: #d9d9d9;
}

.no-session-selected h3 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.no-session-selected p {
  margin: 0;
  font-size: 14px;
}

/* 滚动条样式 */
.sessions-list::-webkit-scrollbar,
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.sessions-list::-webkit-scrollbar-thumb,
.messages-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.sessions-list::-webkit-scrollbar-track,
.messages-container::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-assistant-container {
    flex-direction: column;
    height: calc(100vh - 60px);
    margin: 0;
    border-radius: 0;
    min-height: calc(100vh - 60px);
  }
  
  .sessions-panel {
    width: 100%;
    height: 40%;
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
  }
  
  .chat-panel {
    height: 60%;
  }
  
  .panel-header,
  .chat-header {
    height: 50px;
    padding: 12px 15px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .quick-questions {
    flex-direction: column;
    align-items: center;
  }
  
  .quick-question-btn {
    width: 100%;
    max-width: 300px;
  }
}

@media (max-width: 480px) {
  .sessions-panel {
    height: 35%;
  }
  
  .chat-panel {
    height: 65%;
  }
  
  .panel-header,
  .chat-header {
    height: 45px;
    padding: 10px 12px;
  }
  
  .messages-container {
    padding: 15px;
  }
  
  .input-container {
    padding: 10px 12px;
  }
  
  .message-content {
    max-width: 90%;
  }
  
  .ai-avatar,
  .message-avatar {
    width: 28px;
    height: 28px;
  }
  
  .ai-avatar-large {
    width: 60px;
    height: 60px;
  }
  
  .welcome-message h3 {
    font-size: 20px;
  }
  
  .welcome-message p {
    font-size: 14px;
  }
}
</style>