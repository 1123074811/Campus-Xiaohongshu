<script setup>
import {onMounted, ref} from 'vue'
import request from '../../utils/request'
import { useRouter } from 'vue-router'
import {ElMessage} from "element-plus";
import blog from "@/views/back/Blog.vue";

const users = ref([])
const loadUser = () => {
  request.get('/user').then(res => {
    users.value = res.data
  })
}

const blogs = ref([])
const loadBlog = () => {
  request.get('/blog').then(res => {
    blogs.value = res.data
  })
}

const messages = ref([])
const loadMessage = () => {
  return request.get('/message').then(res => {
    messages.value = res.data
  })
}

const router = useRouter()
const goToUser = (id) => {
  if (!id) return
  router.push({ path: '/front/user', query: { id: String(id) } })
}

const follow = (id) => {
  request.post("/follow", {
    itemId: id
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success("关注成功");
      followedStatus.value[id] = true;
    } else {
      ElMessage.error(res.msg || '关注失败');
      followedStatus.value[id] = false;
    }
  });
};

// 使用对象存储每个用户的关注状态
const followedStatus = ref({})

const checkFollow = (id) => {
  request.get("/follow/checkFollow/" + id).then(res => {
    if (res.code === '200') {
      followedStatus.value[id] = true
    } else {
      followedStatus.value[id] = false
    }
  });
};

// 初始化时检查所有关注消息中用户的关注状态
const initFollowStatus = () => {
  // 找出所有关注类型的消息的发送者ID
  const followerIds = [...new Set(messages.value
    .filter(msg => msg.type === '关注')
    .map(msg => msg.fromUserId))];
  
  // 为每个发送者检查关注状态
  followerIds.forEach(id => checkFollow(id));
};

onMounted(() => {
  loadUser()
  loadBlog()
  loadMessage().then(() => {
    // 消息加载完成后初始化关注状态
    initFollowStatus()
  })
})
</script>

<template>
  <div class="content-container">
    <!-- 标题 -->
    <div class="header-title">
      <h2>互动消息</h2>
    </div>

    <!-- 通知列表 -->
    <div class="notification-list">
      <div v-for="message in messages" :key="message.id" class="notification-item">
        <!-- 用户头像 -->
        <div class="user-avatar">
          <img :src="users.find(user => user.id === message.fromUserId)?.avatarUrl" :alt="users.find(user => user.id === message.fromUserId)?.nickname" style="cursor: pointer" @click="goToUser(message.fromUserId)">
        </div>

        <!-- 通知内容 -->
        <div class="notification-content">
          <div class="user-info">
            <span class="username" style="cursor: pointer" @click="goToUser(message.fromUserId)">{{ users.find(user => user.id === message.fromUserId)?.nickname }}</span>
          </div>
          <div class="notification-text">{{ message.text }}</div>
          <div class="notification-time">{{ message.time }}</div>
        </div>

        <!-- 关联内容缩略图 -->
        <div class="content-thumbnail" v-if="message.itemId !== null">
          <img :src="blogs.find(blog => blog.id === message.itemId)?.img" :alt="blogs.find(blog => blog.id === message.itemId)?.name">
        </div>
        <!-- 如果是被关注了，显示回关按钮 -->
        <button
            v-if="message.type === '关注'"
            class="follow-button"
            :class="{ 'follow-button--followed': followedStatus[message.fromUserId] }"
            @click="follow(message.fromUserId)">
          {{ followedStatus[message.fromUserId] ? '已关注' : '回关' }}
        </button>
      </div>

      <!-- 结束标识 -->
      <div class="end-marker">
        - THE END -
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.content-container {
  width: 100%;
}

.header-title {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fff;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: #333;
  }
}

.notification-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background-color: #fff;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;

  &:hover {
    background-color: #fafafa;
    margin: 0 -20px;
    padding: 16px 20px;
    border-radius: 8px;
  }
}

.user-avatar {
  flex-shrink: 0;

  img {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.notification-content {
  flex: 1;
  min-width: 0;

  .user-info {
    margin-bottom: 4px;

    .username {
      font-weight: 500;
      color: #333;
      font-size: 14px;
    }

  }

  .notification-text {
    color: #666;
    font-size: 14px;
    margin-bottom: 4px;
    line-height: 1.4;
  }

  .notification-time {
    color: #999;
    font-size: 12px;
  }
}

.content-thumbnail {
  flex-shrink: 0;

  img {
    width: 60px;
    height: 60px;
    border-radius: 6px;
    object-fit: cover;
    border: 1px solid #f0f0f0;
    cursor: pointer;
  }
}

.end-marker {
  text-align: center;
  color: #ccc;
  font-size: 12px;
  margin: 40px 0;
  padding: 20px 0;
}

.follow-button {
  background-color: #ff2442;
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    background-color: #e01e3c;
  }

  &--followed {
    background-color: #f0f0f0;
    color: #666;
    border: 1px solid #d9d9d9;
    padding: 7px 19px; /* 调整内边距以补偿1px的边框宽度 */

    &:hover {
      background-color: #e8e8e8;
    }
  }
}
</style>