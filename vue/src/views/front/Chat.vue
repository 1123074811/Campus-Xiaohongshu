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
            <div class="friend-last-message" v-if="friend.lastMessage">{{ friend.lastMessage }}</div>
          </div>
          <div class="friend-time" v-if="friend.lastMessageTime">{{ friend.lastMessageTime }}</div>
        </div>
      </div>
    </div>
    
    <!-- 右侧聊天区域 -->
    <div class="chat-main" v-if="selectedFriend">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="current-friend-info">
          <img :src="selectedFriend.avatarUrl || '/default-avatar.png'" alt="好友头像" class="header-avatar">
          <span class="current-friend-name">{{ selectedFriend.nickname }}</span>
        </div>
      </div>
      
      <!-- 聊天记录区域 -->
      <div class="chat-history" ref="chatHistory">
        <div v-if="chatHistory.length === 0" class="no-messages">
          暂无聊天记录，开始聊天吧！
        </div>
        <transition-group name="message-list" tag="div">
          <template v-for="(msg, index) in chatHistory" :key="msg.id || Date.now() + Math.random() + index">
            <!-- 日期分隔符 -->
            <div 
              v-if="shouldShowDateSeparator(index)"
              class="date-separator"
              :key="`date-${getDateKey(msg.sendTime)}`"
            >
              {{ getDateSeparatorText(msg.sendTime) }}
            </div>
            
            <!-- 消息项 -->
            <div 
              :class="['message-item', msg.fromUid === currentUid ? 'msg-right' : 'msg-left', msg.isNew ? 'message-new' : '']"
              @transitionend="handleTransitionEnd(msg)"
            >
              <div class="msg-content">{{ msg.content }}</div>
              <div class="msg-time">{{ formatTime(msg.sendTime) }}</div>
            </div>
          </template>
        </transition-group>
        
        <!-- 底部提示 -->
        <div v-if="isAtBottom" class="at-bottom-tip">已显示全部消息</div>
      </div>
      
      <!-- 消息输入区域 -->
      <div class="chat-input">
        <input
          v-model="message"
          placeholder="请输入消息..."
          @keyup.enter="sendMessage"
        >
        <button @click="sendMessage" :disabled="!message.trim()">发送</button>
      </div>
    </div>
    
    <!-- 未选择好友时的提示 -->
    <div class="no-selection" v-else>
      <p>请选择一个好友开始聊天</p>
    </div>
  </div>
</template>

<script>
</script>
<style scoped>

</style>