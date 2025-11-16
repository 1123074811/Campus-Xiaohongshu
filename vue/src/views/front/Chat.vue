<template>
  <div class="chat-container">
    <!-- 左侧好友列表 -->
    <div class="friends-list">
      <div class="friends-header">
        <h3>好友列表</h3>
      </div>
      <div class="friends-content">
        <div
            v-for="friend in friends"
            :key="friend.id"
            :class="['friend-item', { active: selectedFriendId === friend.id }]"
            @click="selectFriend(friend)"
        >
          <div class="friend-avatar">
            <img :src="friend.avatarUrl || '/default-avatar.png'" alt="好友头像">
          </div>
          <div class="friend-info">
            <div class="friend-name">{{ friend.nickname }}</div>
            <div class="friend-last-message">{{ friend.lastMessage }}</div>
          </div>
          <div class="friend-time">{{ friend.lastMessageTime }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main" v-if="selectedFriend">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="current-friend-info">
          <img :src="selectedFriend.avatarUrl || '/default-avatar.png'" class="header-avatar">
          <div class="chat-title">
            <div class="current-friend-name">{{ selectedFriend.nickname }}</div>
            <div class="current-friend-status">
              <span :class="['status-dot', isPolling ? 'online' : 'offline']"></span>
              {{ isPolling ? '在线' : '离线' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 聊天记录区域 -->
      <div class="chat-history" ref="chatHistoryEl">
        <div v-if="chatHistory.length === 0" class="no-messages">暂无聊天记录，开始聊天吧！</div>
        <transition-group name="message-list" tag="div">
          <template v-for="(msg, index) in chatHistory" :key="msg.id">
            <div v-if="shouldShowDateSeparator(index)" class="date-separator" :key="`date-${getDateKey(msg.sendTime)}`">
              {{ getDateSeparatorText(msg.sendTime) }}
            </div>
            <div class="message-row" :class="msg.fromUid === currentUid ? 'right' : 'left'" @transitionend="handleTransitionEnd(msg)">
              <img v-if="msg.fromUid !== currentUid" class="msg-avatar" :src="getAvatarFor(msg.fromUid)" alt="">
              <div class="bubble" :class="msg.fromUid === currentUid ? 'right' : 'left'">
                <div class="msg-content">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.sendTime) }}</div>
              </div>
              <img v-if="msg.fromUid === currentUid" class="msg-avatar" :src="getAvatarFor(msg.fromUid)" alt="">
            </div>
          </template>
        </transition-group>
        <div v-if="isAtBottom && chatHistory.length > 0" class="at-bottom-tip">已显示全部消息</div>
      </div>

      <!-- 消息输入区域 -->
      <div class="chat-input">
        <textarea v-model="message" rows="2" placeholder="请输入消息..." @keyup.enter="sendMessage"></textarea>
        <button class="send-btn" @click="sendMessage" :disabled="!message.trim()">发送</button>
      </div>
    </div>

    <!-- 未选择好友时的提示 -->
    <div class="no-selection" v-else>
      <p>请选择一个好友开始聊天</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import request from '@/utils/request'

// 响应式数据
const isPolling = ref(false)
const pollSince = ref(null)
const pollBackoffMs = ref(1000)
const pollMaxBackoffMs = ref(15000)
const suggestedInterval = ref(30000)
const pollController = ref(null)
const currentUid = ref('')
const friends = ref([])
const selectedFriendId = ref('')
const selectedFriend = ref(null)
const chatHistory = ref([])
const friendChatHistories = ref({})
const message = ref('')
const isAtBottom = ref(true)
const chatHistoryEl = ref(null) // 修复：统一模板引用名称

// 生命周期
onMounted(async () => {
  console.log('Chat component mounted')
  currentUid.value = getUserId()
  if (!currentUid.value) {
    try {
      const res = await request.get('/web/userInfo')
      const u = res.data
      if (u && u.id != null) {
        const acc = {
          id: u.id,
          nickname: u.nickname,
          avatarUrl: u.avatarUrl
        }
        localStorage.setItem('account', JSON.stringify(acc))
        currentUid.value = String(u.id)
        console.log('Current user ID:', currentUid.value)
      }
    } catch (e) {
      console.error('Failed to get user info:', e)
    }
  }
  await loadFriendsList()
})

onBeforeUnmount(() => {
  isPolling.value = false
  if (pollController.value) pollController.value.abort()
})

// 方法
const loadFriendsList = async () => {
  try {
    console.log('Loading friends list...')
    const res = await request.get('/follow/friend/list')
    const arr = res.data?.friends || []
    friends.value = arr.map(x => ({
      id: String(x.user_id),
      nickname: x.nickname || '未知用户',
      avatarUrl: x.avatar_url,
      lastMessage: '',
      lastMessageTime: ''
    }))
    console.log('Friends loaded:', friends.value)
  } catch (e) {
    console.error('Failed to load friends list:', e)
  }
}

const selectFriend = async (friend) => {
  console.log('Selecting friend:', friend)
  selectedFriendId.value = friend.id
  selectedFriend.value = friend
  await loadChatHistory()
  pollSince.value = null
  startPolling()
  reportRead()
}

const loadChatHistory = async () => {
  if (!selectedFriendId.value) {
    console.log('No friend selected')
    return
  }

  try {
    console.log('Loading chat history for friend:', selectedFriendId.value)
    const res = await request.get('/chat/history', {
      params: {
        uid1: String(currentUid.value),
        uid2: String(selectedFriendId.value)
      }
    })
    console.log('Chat history response:', res)

    const arr = res.data?.messages || []
    console.log('Raw messages:', arr)

    const msgs = arr.map(x => ({
      id: x.id || generateMsgId(),
      content: x.content || '',
      fromUid: String(x.fromUid),
      toUid: String(x.toUid),
      sendTime: normalizeTs(x.sendTime),
      isNew: false
    }))

    console.log('Processed messages:', msgs)
    chatHistory.value = msgs
    friendChatHistories.value[selectedFriendId.value] = [...msgs]

    nextTick(() => {
      scrollToBottom()
    })
  } catch (e) {
    console.error('Failed to load chat history:', e)
  }
}

const startPolling = () => {
  if (isPolling.value) return
  isPolling.value = true
  pollController.value = new AbortController()

  const loop = async () => {
    if (!isPolling.value) return
    try {
      const res = await request.get('/chat/poll', {
        params: {
          since: pollSince.value,
          timeoutSeconds: 30
        },
        timeout: 35000,
        signal: pollController.value.signal
      })
      const data = res.data || res
      const list = data.messages || []
      if (list.length > 0) {
        list.forEach(m => handleChatMessage({
          fromUserId: m.fromUid,
          toUserId: m.toUid,
          content: m.content,
          timestamp: normalizeTs(m.sendTime)
        }))
        pollSince.value = data.next_since || list[list.length - 1].id
      }
      suggestedInterval.value = (data.suggested_poll_interval || 30) * 1000
      pollBackoffMs.value = 1000
      setTimeout(loop, 0)
    } catch (e) {
      if (e?.code === 'ERR_CANCELED') return
      console.error('Polling error:', e)
      pollBackoffMs.value = Math.min(pollBackoffMs.value * 2, pollMaxBackoffMs.value)
      setTimeout(loop, pollBackoffMs.value)
    }
  }
  loop()
}

const sendMessage = async () => {
  const content = message.value.trim()
  if (!content || !selectedFriendId.value) return
  try {
    const clientMsgId = `c_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    const res = await request.post('/chat/send', {
      to_user_id: String(selectedFriendId.value),
      content,
      client_msg_id: clientMsgId
    }, { timeout: 10000 })

    console.log('Send message response:', res)

    const serverTs = normalizeTs(res.data?.created_at ?? res.data?.sendTime ?? Date.now())
    const sentMessage = {
      id: res.data?.id || generateMsgId(),
      content,
      fromUid: currentUid.value,
      toUid: selectedFriendId.value,
      sendTime: serverTs,
      isNew: false
    }

    chatHistory.value.push(sentMessage)
    if (!friendChatHistories.value[selectedFriendId.value]) {
      friendChatHistories.value[selectedFriendId.value] = []
    }
    friendChatHistories.value[selectedFriendId.value].push(sentMessage)

    scrollToBottom()
    message.value = ''
    updateFriendLastMessage(selectedFriendId.value, content)
  } catch (e) {
    console.error('Failed to send message:', e)
  }
}

const handleChatMessage = (message) => {
  const chatMessage = {
    id: generateMsgId(),
    content: message.content || '',
    fromUid: String(message.fromUserId),
    toUid: String(message.toUserId),
    sendTime: message.timestamp || Date.now(),
    isNew: true
  }

  console.log('Handling chat message:', chatMessage)

  if (selectedFriendId.value === String(message.fromUserId)) {
    chatHistory.value.push(chatMessage)
    if (!friendChatHistories.value[selectedFriendId.value]) {
      friendChatHistories.value[selectedFriendId.value] = []
    }
    friendChatHistories.value[selectedFriendId.value].push(chatMessage)
    scrollToBottom()
  } else {
    updateFriendLastMessage(String(message.fromUserId), message.content)
  }
}

const updateFriendLastMessage = (friendId, lastMessage) => {
  const friend = friends.value.find(f => f.id === friendId)
  if (friend) {
    friend.lastMessage = lastMessage
    friend.lastMessageTime = formatTime(Date.now())
  }
}

const getAvatarFor = (uid) => {
  const f = friends.value.find(x => String(x.id) === String(uid))
  if (f && f.avatarUrl) return f.avatarUrl
  const account = localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : null
  if (String(uid) === String(currentUid.value)) {
    return account?.avatarUrl || '/default-avatar.png'
  }
  return '/default-avatar.png'
}

const reportRead = async () => {
  if (!selectedFriendId.value) return
  try {
    await request.post('/chat/ack/read', {
      from_user_id: String(selectedFriendId.value)
    })
  } catch (e) {
    console.error('Failed to report read:', e)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistoryEl.value) {
      chatHistoryEl.value.scrollTop = chatHistoryEl.value.scrollHeight
      isAtBottom.value = true
    }
  })
}

const shouldShowDateSeparator = (index) => {
  if (index === 0) return true
  const prev = chatHistory.value[index - 1]
  const curr = chatHistory.value[index]
  if (!prev || !curr) return false
  return getDateKey(prev.sendTime) !== getDateKey(curr.sendTime)
}

const getDateKey = (timestamp) => {
  const t = normalizeTs(timestamp)
  if (!Number.isFinite(t)) return ''
  return new Date(t).toDateString()
}

const getDateSeparatorText = (timestamp) => {
  const t = normalizeTs(timestamp)
  if (!Number.isFinite(t)) return ''
  return new Date(t).toLocaleDateString()
}

const formatTime = (timestamp) => {
  const t = normalizeTs(timestamp)
  if (!Number.isFinite(t)) return ''
  const date = new Date(t)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleTransitionEnd = (msg) => {
  if (msg.isNew) msg.isNew = false
}

const getUserId = () => {
  const userStr = localStorage.getItem('account')
  if (!userStr) return ''
  try {
    const user = JSON.parse(userStr)
    return user?.id != null ? String(user.id) : ''
  } catch (_) {
    return ''
  }
}

const normalizeTs = (ts) => {
  if (typeof ts === 'number') return ts
  if (typeof ts === 'string') {
    // 处理 ISO 格式时间字符串
    const m = ts.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?(?:Z)?$/)
    if (m) {
      const [_, y, mo, d, h, mi, s, ms] = m
      const msInt = ms ? parseInt(ms.padEnd(3, '0'), 10) : 0
      return new Date(parseInt(y,10), parseInt(mo,10)-1, parseInt(d,10), parseInt(h,10), parseInt(mi,10), parseInt(s,10), msInt).getTime()
    }
    const n = Date.parse(ts)
    return Number.isNaN(n) ? NaN : n
  }
  try {
    const n = new Date(ts).getTime()
    return Number.isNaN(n) ? NaN : n
  } catch (_) {
    return NaN
  }
}

const generateMsgId = () => {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}
</script>
<style scoped>
.chat-container {
  display: flex;
  height: calc(100dvh - var(--app-header-height, 64px));
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 左侧好友列表样式 */
.friends-list {
  width: 300px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.friends-header {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.friends-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.friends-content {
  flex: 1;
  overflow-y: auto;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.friend-item:hover {
  background-color: #f8f9fa;
}

.friend-item.active {
  background-color: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  background-color: #e0e0e0;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #f8f9fa;
}

.current-friend-info {
  display: flex;
  align-items: center;
}

.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #e0e0e0;
}

.chat-title {
  display: flex;
  flex-direction: column;
  margin-left: 12px;
}

.current-friend-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.current-friend-status {
  display: flex;
  align-items: center;
  margin-top: 2px;
  font-size: 12px;
  color: #666;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  background-color: #9e9e9e;
}

.status-dot.online { background-color: #4caf50; }
.status-dot.offline { background-color: #9e9e9e; }

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0 8px;
  background-color: #fff;
  min-height: 0;
}

.message-row {
  display: flex;
  margin: 6px 16px;
}
.message-row.left { justify-content: flex-start; }
.message-row.right { justify-content: flex-end;}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #e0e0e0;
  object-fit: cover;
  margin: 0 8px;
}

.bubble {
  max-width: 68%;
  padding: 10px 12px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.bubble.left {
  background-color: #ffffff;
  border: 1px solid #eee;
}
.bubble.right { background-color: #e3f2fd; }

.msg-content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #333;
  font-size: 14px;
}

.msg-time {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.chat-input {
  border-top: 1px solid #e0e0e0;
  background-color: #fafafa;
  padding: 10px 12px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  position: sticky;
  bottom: 0;
  z-index: 5;
}

.chat-input textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  resize: none;
}

.chat-input textarea:focus {
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.send-btn {
  padding: 10px 18px;
  background-color: #2196f3;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-weight: 500;
}
.send-btn:hover:not(:disabled) { background-color: #1976d2; }
.send-btn:disabled { background-color: #ccc; cursor: not-allowed; }

.friend-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-time {
  font-size: 11px;
  color: #999;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.date-separator {
  text-align: center;
  margin: 16px 0;
  color: #999;
  font-size: 12px;
  position: relative;
}

.date-separator::before,
.date-separator::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background-color: #ddd;
}

.date-separator::before {
  left: 0;
}

.date-separator::after {
  right: 0;
}

.at-bottom-tip {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 8px 0;
}

.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background-color: #fafafa;
}
</style>