<template>
  <div class="ai-assistant-management">
    <div class="page-header">
      <h2>AI助手管理</h2>
      <p>管理AI助手的会话记录、用户统计和系统配置</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon :size="32" color="#409EFF"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalSessions }}</div>
              <div class="stat-label">总会话数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon :size="32" color="#67C23A"><Message /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ stats.totalMessages }}</div>
              <div class="stat-label">总消息数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon :size="32" color="#E6A23C"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ stats.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon :size="32" color="#F56C6C"><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ stats.todaySessions }}</div>
              <div class="stat-label">今日会话</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 功能选项卡 -->
    <el-tabs v-model="activeTab" class="management-tabs">
      <!-- 会话管理 -->
      <el-tab-pane label="会话管理" name="sessions">
        <div class="tab-content">
          <!-- 搜索和筛选 -->
          <div class="search-bar">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :md="6">
                <el-input
                    v-model="sessionSearch.keyword"
                    placeholder="搜索会话标题或用户昵称"
                    clearable
                    @input="handleSessionSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </el-col>
              <el-col :xs="24" :sm="12" :md="7">
                <el-date-picker
                    v-model="sessionSearch.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="handleSessionSearch"
                    style="width: 100%;"
                />
              </el-col>
              <el-col :span="4">
                <el-button type="primary" @click="handleSessionSearch">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </el-col>
              <el-col :span="6">
                <div class="action-buttons">
                  <el-button type="danger" @click="handleBatchDelete" :disabled="selectedSessions.length === 0">
                    <el-icon><Delete /></el-icon>
                    批量删除
                  </el-button>
                  <el-button @click="handleRefresh">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 会话列表 -->
          <el-table
              :data="sessions"
              v-loading="sessionsLoading"
              @selection-change="handleSessionSelectionChange"
              class="sessions-table"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="sessionId" label="会话ID" width="120" show-overflow-tooltip />
            <el-table-column prop="title" label="会话标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="userNickname" label="用户" width="120" />
            <el-table-column prop="messageCount" label="消息数" width="80" align="center" />
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="最后活动" width="180">
              <template #default="{ row }">
                {{ formatTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewSessionMessages(row)">
                  <el-icon><View /></el-icon>
                  查看消息
                </el-button>
                <el-button type="danger" size="small" @click="deleteSession(row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
                v-model:current-page="sessionPagination.page"
                v-model:page-size="sessionPagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="sessionPagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadSessions"
                @current-change="loadSessions"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 消息管理 -->
      <el-tab-pane label="消息管理" name="messages">
        <div class="tab-content">
          <!-- 消息搜索 -->
          <div class="search-bar">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-input
                    v-model="messageSearch.keyword"
                    placeholder="搜索消息内容"
                    clearable
                    @input="handleMessageSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
              </el-col>
              <el-col :span="5">
                <el-select v-model="messageSearch.messageType" placeholder="消息类型" clearable @change="handleMessageSearch">
                  <el-option label="用户消息" value="USER" />
                  <el-option label="AI回复" value="ASSISTANT" />
                </el-select>
              </el-col>
              <el-col :span="7">
                <el-date-picker
                    v-model="messageSearch.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    @change="handleMessageSearch"
                />
              </el-col>
              <el-col :span="6">
                <el-button type="primary" @click="handleMessageSearch">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </el-col>
            </el-row>
          </div>

          <!-- 消息列表 -->
          <el-table
              :data="messages"
              v-loading="messagesLoading"
              class="messages-table"
          >
            <el-table-column prop="sessionId" label="会话ID" width="120" show-overflow-tooltip />
            <el-table-column prop="userNickname" label="用户" width="120" />
            <el-table-column prop="messageType" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.messageType === 'USER' ? 'primary' : 'success'" size="small">
                  {{ row.messageType === 'USER' ? '用户' : 'AI' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="消息内容" min-width="300">
              <template #default="{ row }">
                <div class="message-content" :class="{ 'ai-message': row.messageType === 'ASSISTANT' }">
                  {{ truncateText(row.content, 100) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发送时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewMessageDetail(row)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
                v-model:current-page="messagePagination.page"
                v-model:page-size="messagePagination.size"
                :page-sizes="[10, 20, 50, 100]"
                :total="messagePagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadMessages"
                @current-change="loadMessages"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 系统配置 -->
      <el-tab-pane label="系统配置" name="config">
        <div class="tab-content">
          <el-form :model="aiConfig" label-width="150px" class="config-form">
            <el-card class="config-card">
              <template #header>
                <span>AI模型配置</span>
              </template>
              <el-form-item label="模型名称">
                <el-input v-model="aiConfig.modelName" placeholder="请输入AI模型名称" />
              </el-form-item>
              <el-form-item label="API地址">
                <el-input v-model="aiConfig.apiUrl" placeholder="请输入API地址" />
              </el-form-item>
              <el-form-item label="系统提示词">
                <el-input
                    v-model="aiConfig.systemPrompt"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入系统提示词"
                />
              </el-form-item>
            </el-card>

            <el-card class="config-card">
              <template #header>
                <span>聊天配置</span>
              </template>
              <el-form-item label="最大消息历史">
                <el-input-number v-model="aiConfig.maxMessages" :min="1" :max="100" />
              </el-form-item>
              <el-form-item label="会话过期时间">
                <el-input-number v-model="aiConfig.sessionExpireHours" :min="1" :max="168" />
                <span class="form-help">小时</span>
              </el-form-item>
              <el-form-item label="启用思考显示">
                <el-switch v-model="aiConfig.enableThinking" />
              </el-form-item>
            </el-card>

            <div class="config-actions">
              <el-button type="primary" @click="saveConfig" :loading="configSaving">
                <el-icon><Check /></el-icon>
                保存配置
              </el-button>
              <el-button @click="resetConfig">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 消息详情对话框 -->
    <el-dialog
        v-model="messageDetailVisible"
        title="消息详情"
        width="60%"
        :before-close="closeMessageDetail"
    >
      <div v-if="currentMessage" class="message-detail">
        <div class="detail-header">
          <el-tag :type="currentMessage.messageType === 'USER' ? 'primary' : 'success'">
            {{ currentMessage.messageType === 'USER' ? '用户消息' : 'AI回复' }}
          </el-tag>
          <span class="detail-time">{{ formatTime(currentMessage.createTime) }}</span>
        </div>
        <div class="detail-content">
          <div class="content-text" v-html="formatMessageContent(currentMessage.content)"></div>
        </div>
        <div class="detail-info">
          <p><strong>会话ID:</strong> {{ currentMessage.sessionId }}</p>
          <p><strong>用户:</strong> {{ currentMessage.userNickname }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- 会话消息对话框 -->
    <el-dialog
        v-model="sessionMessagesVisible"
        title="会话消息记录"
        width="80%"
        :before-close="closeSessionMessages"
    >
      <div v-if="currentSessionMessages.length > 0" class="session-messages">
        <div
            v-for="(message, index) in currentSessionMessages"
            :key="index"
            class="session-message"
            :class="{ 'user-message': message.messageType === 'USER', 'ai-message': message.messageType === 'ASSISTANT' }"
        >
          <div class="message-header">
            <el-tag :type="message.messageType === 'USER' ? 'primary' : 'success'" size="small">
              {{ message.messageType === 'USER' ? '用户' : 'AI' }}
            </el-tag>
            <span class="message-time">{{ formatTime(message.createTime) }}</span>
          </div>
          <div class="message-body" v-html="formatMessageContent(message.content)"></div>
        </div>
      </div>
      <el-empty v-else description="暂无消息记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound, Message, User, Calendar, Search, Delete, Refresh, View, Check
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// 响应式数据
const activeTab = ref('sessions')
const stats = ref({
  totalSessions: 0,
  totalMessages: 0,
  activeUsers: 0,
  todaySessions: 0
})

// 会话管理相关
const sessions = ref([])
const sessionsLoading = ref(false)
const selectedSessions = ref([])
const sessionSearch = ref({
  keyword: '',
  dateRange: null
})
const sessionPagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 消息管理相关
const messages = ref([])
const messagesLoading = ref(false)
const messageSearch = ref({
  keyword: '',
  messageType: '',
  dateRange: null
})
const messagePagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 消息详情
const messageDetailVisible = ref(false)
const currentMessage = ref(null)

// 会话消息
const sessionMessagesVisible = ref(false)
const currentSessionMessages = ref([])

// 系统配置
const aiConfig = ref({
  modelName: 'deepseek-r1:7b',
  apiUrl: 'http://localhost:11434',
  systemPrompt: '你是一个热心、可爱的智能助手，叫哈基聪，请以哈基聪的语气回答问题',
  maxMessages: 20,
  sessionExpireHours: 24,
  enableThinking: true
})
const configSaving = ref(false)

// 计算属性
const uploadHeaders = computed(() => {
  const currentAccount = localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : null
  return currentAccount && currentAccount.token ? {
    'token': currentAccount.token
  } : {}
})

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('代码高亮错误:', err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true,
})

// 生命周期
onMounted(() => {
  loadStats()
  loadSessions()
  loadMessages()
  loadConfig()
})

// 方法
const loadStats = async () => {
  try {
    const res = await request.get('/admin/ai/stats', {
      headers: uploadHeaders.value
    })
    stats.value = res.data || stats.value
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadSessions = async () => {
  sessionsLoading.value = true
  try {
    const params = {
      page: sessionPagination.value.page,
      size: sessionPagination.value.size,
      keyword: sessionSearch.value.keyword,
      startDate: sessionSearch.value.dateRange?.[0],
      endDate: sessionSearch.value.dateRange?.[1]
    }

    const res = await request.get('/admin/ai/sessions', {
      params,
      headers: uploadHeaders.value
    })

    sessions.value = res.data.records || []
    sessionPagination.value.total = res.data.total || 0
  } catch (error) {
    console.error('加载会话列表失败:', error)
    ElMessage.error('加载会话列表失败')
  } finally {
    sessionsLoading.value = false
  }
}

const loadMessages = async () => {
  messagesLoading.value = true
  try {
    const params = {
      page: messagePagination.value.page,
      size: messagePagination.value.size,
      keyword: messageSearch.value.keyword,
      messageType: messageSearch.value.messageType,
      startDate: messageSearch.value.dateRange?.[0],
      endDate: messageSearch.value.dateRange?.[1]
    }

    const res = await request.get('/admin/ai/messages', {
      params,
      headers: uploadHeaders.value
    })

    messages.value = res.data.records || []
    messagePagination.value.total = res.data.total || 0
  } catch (error) {
    console.error('加载消息列表失败:', error)
    ElMessage.error('加载消息列表失败')
  } finally {
    messagesLoading.value = false
  }
}

const loadConfig = async () => {
  try {
    const res = await request.get('/admin/ai/config', {
      headers: uploadHeaders.value
    })
    if (res.data) {
      aiConfig.value = { ...aiConfig.value, ...res.data }
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

const handleSessionSearch = () => {
  sessionPagination.value.page = 1
  loadSessions()
}

const handleMessageSearch = () => {
  messagePagination.value.page = 1
  loadMessages()
}

const handleSessionSelectionChange = (selection) => {
  selectedSessions.value = selection
}

const handleBatchDelete = async () => {
  if (selectedSessions.value.length === 0) {
    ElMessage.warning('请选择要删除的会话')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedSessions.value.length} 个会话吗？删除后无法恢复。`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    )

    const sessionIds = selectedSessions.value.map(s => s.sessionId)
    await request.delete('/admin/ai/sessions/batch', {
      data: { sessionIds },
      headers: uploadHeaders.value
    })

    ElMessage.success('批量删除成功')
    loadSessions()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const handleRefresh = () => {
  loadSessions()
  loadMessages()
  loadStats()
}

const deleteSession = async (session) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除会话"${session.title}"吗？删除后无法恢复。`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
    )

    await request.delete(`/admin/ai/sessions/${session.sessionId}`, {
      headers: uploadHeaders.value
    })

    ElMessage.success('删除成功')
    loadSessions()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error('删除会话失败')
    }
  }
}

const viewSessionMessages = async (session) => {
  try {
    const res = await request.get(`/admin/ai/sessions/${session.sessionId}/messages`, {
      headers: uploadHeaders.value
    })
    currentSessionMessages.value = res.data || []
    sessionMessagesVisible.value = true
  } catch (error) {
    console.error('加载会话消息失败:', error)
    ElMessage.error('加载会话消息失败')
  }
}

const viewMessageDetail = (message) => {
  currentMessage.value = message
  messageDetailVisible.value = true
}

const closeMessageDetail = () => {
  messageDetailVisible.value = false
  currentMessage.value = null
}

const closeSessionMessages = () => {
  sessionMessagesVisible.value = false
  currentSessionMessages.value = []
}

const saveConfig = async () => {
  configSaving.value = true
  try {
    await request.post('/admin/ai/config', aiConfig.value, {
      headers: uploadHeaders.value
    })
    ElMessage.success('配置保存成功')
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败')
  } finally {
    configSaving.value = false
  }
}

const resetConfig = () => {
  loadConfig()
  ElMessage.info('配置已重置')
}

// 工具方法
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  // 后端已经格式化为 YYYY-MM-DD HH:mm:ss 格式，直接返回
  return timeStr
}

const truncateText = (text, maxLength) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const formatMessageContent = (content) => {
  if (!content) return ''

  // 处理思考内容
  let processedContent = content

  const hasThink = content.includes('<think>') && content.includes('</think>')
  const hasThinking = content.includes('<thinking>') && content.includes('</thinking>')

  if (hasThink || hasThinking) {
    // 提取思考内容
    const thinkMatches = content.matchAll(/<think>([\s\S]*?)<\/think>|<thinking>([\s\S]*?)<\/thinking>/g)
    const thinkContents = []
    for (const match of thinkMatches) {
      thinkContents.push(match[1] || match[2])
    }

    // 移除思考标签
    processedContent = content
        .replace(/<think>[\s\S]*?<\/think>/g, '')
        .replace(/<thinking>[\s\S]*?<\/thinking>/g, '')
        .trim()

    // 添加思考块
    if (thinkContents.length > 0) {
      const mergedThinking = thinkContents.join('\n\n').trim()
      const escapedThinking = mergedThinking
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/\n/g, '<br>')
      const thinkingBlock = `<div class="thinking-content"><div class="thinking-header">🤔 AI思考过程</div><div class="thinking-text">${escapedThinking}</div></div>`

      if (processedContent) {
        processedContent = thinkingBlock + '<br><br>' + processedContent
      } else {
        processedContent = thinkingBlock
      }
    }
  }

  // 检查是否包含思考块
  const hasSeparatedThinking = processedContent.includes('<div class="thinking-content">')

  if (hasSeparatedThinking) {
    const thinkingMatch = processedContent.match(/(<div class="thinking-content">.*?<\/div><\/div>)(<br><br>)?(.*)/s)
    if (thinkingMatch) {
      const thinkingBlock = thinkingMatch[1]
      const mainContent = thinkingMatch[3] || ''

      const renderedContent = mainContent.trim() ? marked.parse(mainContent) : ''

      return thinkingBlock + (renderedContent ? '<br><br>' + renderedContent : '')
    }
  }

  // 使用 marked 渲染 markdown
  try {
    return marked.parse(processedContent)
  } catch (error) {
    console.error('Markdown渲染错误:', error)
    return processedContent.replace(/\n/g, '<br>')
  }
}
</script>

<style scoped>
.ai-assistant-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.stats-cards {
  margin-bottom: 30px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.management-tabs {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.tab-content {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.sessions-table,
.messages-table {
  margin-bottom: 20px;
}

.message-content {
  max-height: 60px;
  overflow: hidden;
  line-height: 1.5;
}

.message-content.ai-message {
  color: #67C23A;
  font-style: italic;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.config-form {
  max-width: 800px;
}

.config-card {
  margin-bottom: 20px;
}

.form-help {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}

.config-actions {
  text-align: center;
  margin-top: 30px;
}

.message-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.detail-time {
  color: #999;
  font-size: 14px;
}

.detail-content {
  margin-bottom: 20px;
}

.content-text {
  line-height: 1.6;
  color: #333;
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}

.detail-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}

.detail-info p {
  margin: 8px 0;
  color: #666;
}

.session-messages {
  max-height: 600px;
  overflow-y: auto;
}

.session-message {
  margin-bottom: 20px;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #ddd;
}

.session-message.user-message {
  background: #f0f7ff;
  border-left-color: #409EFF;
}

.session-message.ai-message {
  background: #f0f9ff;
  border-left-color: #67C23A;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-body {
  line-height: 1.6;
  color: #333;
}

/* 思考内容样式 */
.content-text :deep(.thinking-content) {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
}

.content-text :deep(.thinking-header) {
  background-color: #e3f2fd;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: #1976d2;
  border-bottom: 1px solid #bbdefb;
}

.content-text :deep(.thinking-text) {
  padding: 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #555;
  font-style: italic;
}

/* Markdown样式 */
.content-text :deep(h1),
.content-text :deep(h2),
.content-text :deep(h3),
.content-text :deep(h4),
.content-text :deep(h5),
.content-text :deep(h6),
.message-body :deep(h1),
.message-body :deep(h2),
.message-body :deep(h3),
.message-body :deep(h4),
.message-body :deep(h5),
.message-body :deep(h6) {
  margin: 16px 0 12px;
  font-weight: 600;
  line-height: 1.4;
  color: #333;
}

.content-text :deep(h1),
.message-body :deep(h1) { font-size: 24px; }
.content-text :deep(h2),
.message-body :deep(h2) { font-size: 20px; }
.content-text :deep(h3),
.message-body :deep(h3) { font-size: 18px; }
.content-text :deep(h4),
.message-body :deep(h4) { font-size: 16px; }
.content-text :deep(h5),
.message-body :deep(h5) { font-size: 14px; }
.content-text :deep(h6),
.message-body :deep(h6) { font-size: 13px; }

.content-text :deep(p),
.message-body :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
}

.content-text :deep(ul),
.content-text :deep(ol),
.message-body :deep(ul),
.message-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.content-text :deep(li),
.message-body :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
}

.content-text :deep(blockquote),
.message-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 12px;
  border-left: 4px solid #409EFF;
  background-color: #f8f9fa;
  color: #555;
  font-style: italic;
}

.content-text :deep(pre),
.message-body :deep(pre) {
  margin: 12px 0;
  padding: 12px;
  background-color: #1e1e1e;
  border-radius: 6px;
  overflow-x: auto;
}

.content-text :deep(pre code),
.message-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.content-text :deep(code),
.message-body :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  color: #e83e8c;
}

.content-text :deep(table),
.message-body :deep(table) {
  margin: 12px 0;
  border-collapse: collapse;
  width: 100%;
}

.content-text :deep(table th),
.content-text :deep(table td),
.message-body :deep(table th),
.message-body :deep(table td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.content-text :deep(table th),
.message-body :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.content-text :deep(table tr:nth-child(even)),
.message-body :deep(table tr:nth-child(even)) {
  background-color: #fafafa;
}

.content-text :deep(a),
.message-body :deep(a) {
  color: #409EFF;
  text-decoration: none;
}

.content-text :deep(a:hover),
.message-body :deep(a:hover) {
  text-decoration: underline;
}

.content-text :deep(hr),
.message-body :deep(hr) {
  margin: 16px 0;
  border: none;
  border-top: 1px solid #e8e8e8;
}

.content-text :deep(img),
.message-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 8px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-assistant-management {
    padding: 10px;
  }

  .search-bar .el-row {
    flex-direction: column;
  }

  .search-bar .el-col {
    width: 100%;
    margin-bottom: 10px;
  }

  .action-buttons {
    justify-content: center;
  }

  .config-form {
    max-width: 100%;
  }
}
</style>