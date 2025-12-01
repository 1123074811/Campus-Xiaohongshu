<script setup>
import {ref, onBeforeUnmount, nextTick} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import request from '../../utils/request'
import {ip, serverHost} from '../../../config/config.default'
import {ElMessage} from 'element-plus'
import {ArrowLeft, ChatRound, UploadFilled, Picture, Position} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})
const chatUser = ref({})
const users = ref([])
const userIds = ref([])
const messages = ref([])
const text = ref('')
const userId = ref(route.query.userId)
const messagesContainer = ref(null)

const loadUser = () => {
  request.get('/chat/user').then(res => {
    users.value = res.data
    init()
    //如果是聊一聊进来的，那么判断一下是否和当前用户沟通过
    if (userId.value) {
      //如果一个也没沟通过，直接创建一个新的对话
      if (users.value.length === 0) {
        request.get('/chat/user/' + userId.value).then(res => {
          const user = res.data
          users.value.unshift(res.data)
          changeUser(user)
          init()
        })
      } else {
        //如果以前和人沟通过，判断以前是否是存在对话记录
        let user = users.value.find(item => item.id === Number(userId.value))
        //没有对话记录，创建一个新的对话
        if (!user) {
          request.get('/chat/user/' + userId.value).then(res => {
            user = res.data
            users.value.unshift(res.data)
            changeUser(user)
            init()
          })
        } else {
          //如果沟通过，直接跳转
          changeUser(user)
          init()
        }
      }
    }
  })
}
loadUser()

let socket = null

const changeUser = (user) => {

  if (chatUser.value.id !== user.id) {
    chatUser.value = user
    users.value.find(item => item.id === user.id).count = 0
    loadMessage(account.value.id, chatUser.value.id)
  }

}

const loadMessage = (fromUserId, toUserId) => {
  request.get('/chat/message', {
    params: {
      fromUserId: fromUserId,
      toUserId: toUserId
    }
  }).then(res => {
    messages.value = res.data
    scrollToBottom()
    clear()
  })
}

const init = () => {
  const userId = account.value.id
  const socketUrl = "ws://"+ip+":9090/chatServer/" + userId;

  // 开启一个websocket服务
  socket = new WebSocket(socketUrl)

  // 打开事件
  socket.onopen = () => {
    console.log("websocket已打开")
  }

  // 接受消息事件
  socket.onmessage = (msg) => {
    const data = JSON.parse(msg.data)
    //如果发来的数据类型是广播，广播给所有在线连接:当前所有在线用户id
    if (data.messageType === 'broadcast') {
      userIds.value = data.userIds
      users.value.forEach(item => {
        // 如果用户id在userIds数组中，设置online为true，否则为false
        item.online = userIds.value.includes(item.id)
      })
    }
    //如果发来的数据类型是聊天，正常执行聊天渲染逻辑
    else if (data.messageType === 'chat') {
      // 只有发来数据用户是和当前正在聊天用户匹配时候，再去给页面上加数据
      if (data.fromUserId === chatUser.value.id) {
        const message = {
          text: data.text,
          type: data.type,
          time: data.time,
          fromUserId: data.fromUserId,
          toUserId: data.toUserId,
          isRead: false,
        }
        createMessage(message)
      } else {
        //否则需要先判断聊天列表里是否有历史记录
        let user = users.value.find(item => item.id === Number(data.fromUserId))
        //如果有，则直接设置未读+1
        if (user) {
          users.value.find(item => item.id === data.fromUserId).count++
        } else {
          //如果没有，就开一个新会话,并且判断一下，这个用户是否在线，设置在线情况，同时未读设置为1
          request.get('/chat/user/' + data.fromUserId).then(res => {
            user = res.data
            user.count = 1
            user.online = userIds.value.includes(data.fromUserId)
            users.value.unshift(user)
          })
        }
      }
    }
  }

  // 错误事件
  socket.onerror = (error) => {
    console.error("WebSocket错误:", error)
  }
}

const sendMessage = () => {
  if (!chatUser.value.id) {
    ElMessage({type: 'warning', message: "请选择聊天对象"})
    return
  }
  if (!text.value) {
    ElMessage({type: 'warning', message: "请输入内容"})
    return
  }

  const message = {
    text: text.value,
    type: '文字',
    time: new Date().toLocaleString('zh-cn'),
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    isRead: false
  }

  socket.send(JSON.stringify(message))
  createMessage(message)
  saveMessage(message)
  text.value = ''
}

const sendImgMessage = (res) => {
  const message = {
    text: res,
    type: '图片',
    time: new Date().toLocaleString('zh-cn'),
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    isRead: false
  }
  socket.send(JSON.stringify(message))
  createMessage(message)
  saveMessage(message)
}

const saveMessage = (message) => {
  request.post('/chat', message).then(res => {
    clear()
  })
}

const createMessage = (message) => {
  messages.value.push(message)
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const clear = () => {
  request.get('/chat/clear', {
    params: {
      fromUserId: account.value.id,
      toUserId: chatUser.value.id
    }
  })
}

onBeforeUnmount(() => {
  if (socket) {
    socket.onclose = () => {
      console.log("websocket已关闭")
    }
    socket.close()
  }
})

const handleKeydown = (e) => {
  if (e.ctrlKey && e.key === 'Enter') {
    sendMessage()
  }
}
</script>

<template>
  <div class="chat-container">
    <div class="users-panel">
      <div class="panel-header">
        <div class="header-left">
          <h3>聊天列表</h3>
        </div>
        <el-badge :value="users.length" class="user-badge" type="primary"/>
      </div>

      <div class="users-list">
        <div
            v-for="user in users"
            :key="user.id"
            class="user-item"
            :class="{ 'active': chatUser.id === user.id }"
            @click="changeUser(user)"
        >
          <div class="user-avatar">
            <img :src="user.avatarUrl" alt="用户头像">
            <span class="user-status" :class="{ 'online': user.online, 'offline': !user.online }"></span>
            <el-badge
                v-if="user.count > 0"
                :value="user.count"
                :max="99"
                class="unread-badge"
                type="danger"
            />
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.nickname }}</div>
            <div class="user-status-text">
              <span :class="user.online ? 'status-online' : 'status-offline'">
                {{ user.online ? '在线' : '离线' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-panel">
      <template v-if="chatUser.id">
        <div class="chat-header">
          <div class="user-info">
            <span class="user-name">{{ chatUser.nickname }}</span>
          </div>
        </div>

        <div class="messages-container" ref="messagesContainer">
          <div
              v-for="(message, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="{ 'message-self': message.fromUserId === account.id }"
          >
            <div class="message-avatar" v-if="message.fromUserId !== account.id">
              <img :src="users.find(item=>item.id===message.fromUserId)?.avatarUrl" alt="头像">
            </div>

            <div class="message-content">
              <div class="message-sender" v-if="message.fromUserId !== account.id">
                {{ users.find(item => item.id === message.fromUserId)?.nickname }}
              </div>

              <div
                  v-if="message.type === '文字'"
                  class="message-bubble"
                  :class="{ 'bubble-self': message.fromUserId === account.id, 'bubble-other': message.fromUserId !== account.id }"
              >{{ message.text }}
              </div>

              <div
                  v-else-if="message.type === '图片'"
                  class="message-image-wrapper"
                  :class="{ 'image-self': message.fromUserId === account.id }"
              >
                <el-image
                    :src="message.text"
                    fit="cover"
                    class="message-image"
                    :preview-src-list="[message.text]"
                    preview-teleported
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon>
                        <Picture/>
                      </el-icon>
                      <span>加载失败</span>
                    </div>
                  </template>
                </el-image>
              </div>

              <div class="message-time">{{ message.time }}</div>
            </div>

            <div class="message-avatar" v-if="message.fromUserId === account.id">
              <img :src="account.avatarUrl" alt="头像">
            </div>

          </div>
        </div>

        <div class="input-container">
          <div class="message-editor">
            <div class="editor-toolbar">
              <el-upload
                  :action="`${serverHost}/web/upload`"
                  :on-success="sendImgMessage"
                  :show-file-list="false"
              >
                <el-button
                    type="default"
                    :icon="UploadFilled"
                    size="small"
                    class="upload-btn"
                >
                  图片
                </el-button>
              </el-upload>
            </div>

            <el-input
                type="textarea"
                v-model="text"
                :rows="3"
                placeholder="请输入消息内容..."
                resize="none"
                @keydown="handleKeydown"
                class="message-textarea"
                @focus="clear"
            />
          </div>

          <div class="send-actions">
            <span class="hint">
              <el-icon size="14"><Position/></el-icon>
              按 Ctrl + Enter 发送
            </span>
            <el-button
                type="primary"
                @click="sendMessage"
                :disabled="!text || !chatUser.id"
            >
              发送消息
            </el-button>
          </div>
        </div>
      </template>

      <div class="no-chat-selected" v-else>
        <el-icon :size="64">
          <ChatRound/>
        </el-icon>
        <h3>请选择一位用户开始聊天</h3>
        <p>从左侧列表选择一位用户开始对话</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
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

.users-panel {
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

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  padding: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.user-badge {
  margin-top: 2px;
}

.users-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 6px;
  position: relative;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-item.active {
  background-color: #f0f7ff;
}

.user-avatar {
  position: relative;
  margin-right: 12px;
}

.user-avatar img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
}

.unread-badge :deep(.el-badge__content) {
  border: 2px solid #fff;
  font-size: 11px;
  height: 18px;
  line-height: 14px;
  padding: 0 5px;
  font-weight: 600;
}

.user-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;
  transition: background-color 0.3s ease;
}

.user-status.online {
  background-color: #52c41a; /* 绿色表示在线 */
}

.user-status.offline {
  background-color: #d9d9d9; /* 灰色表示离线 */
}

.user-info {
  flex: 1;
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.user-status-text {
  font-size: 12px;
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
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9fafc;
  background-image: linear-gradient(rgba(240, 240, 240, 0.5) 1px, transparent 1px),
  linear-gradient(90deg, rgba(240, 240, 240, 0.5) 1px, transparent 1px);
  background-size: 20px 20px;
}

.message-wrapper {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
}

.message-wrapper.message-self {
  justify-content: flex-end;
}

.message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
  flex-shrink: 0;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

.message-wrapper:not(.message-self) .message-content {
  align-items: flex-start;
}

.message-wrapper.message-self .message-content {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  padding: 0 4px;
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

.message-bubble.bubble-other {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 4px;
}

.message-bubble.bubble-self {
  background-color: #1890ff;
  color: #fff;
  border-top-right-radius: 4px;
}

.message-image-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #f5f5f5;
}

.message-image-wrapper.image-self {
  border-top-right-radius: 4px;
}

.message-image-wrapper:not(.image-self) {
  border-top-left-radius: 4px;
}

.message-image {
  width: 200px;
  height: 200px;
  display: block;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.message-image:hover {
  transform: scale(1.02);
}

.message-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-error {
  width: 200px;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
  gap: 8px;
}

.image-error .el-icon {
  font-size: 32px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
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

.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.upload-btn {
  border-radius: 6px;
  font-size: 13px;
  height: 32px;
  padding: 0 16px;
  transition: all 0.3s ease;
  border-color: #d9d9d9;
}

.upload-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
  background-color: #f0f7ff;
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

.no-chat-selected {
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

.no-chat-selected .el-icon {
  margin-bottom: 20px;
  color: #d9d9d9;
}

.no-chat-selected h3 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.no-chat-selected p {
  margin: 0;
  font-size: 14px;
}

.users-list::-webkit-scrollbar,
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.users-list::-webkit-scrollbar-thumb,
.messages-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.users-list::-webkit-scrollbar-track,
.messages-container::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>
