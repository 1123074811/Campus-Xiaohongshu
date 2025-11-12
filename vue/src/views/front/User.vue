<script setup>
import {onMounted, reactive, ref, computed, nextTick} from 'vue'
import request from '../../utils/request'
import {ElMessage} from "element-plus";
import {Star, StarFilled} from "@element-plus/icons-vue";

import {useRoute} from "vue-router";

const route = useRoute()
// 优先从路由参数获取ID，如果没有则从localStorage获取当前登录用户ID
const id = ref(route.query.id || (localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')).id : null))

const userInfo = ref({})
const loadUserInfo = () => {
  if (id.value) {
    request.get('/user/' + id.value).then(res => {
      userInfo.value = res.data
    })
  }
}

const userInfoCount = ref({})
const loadUserInfoCount = () => {
  if (id.value) {
    request.get('/user/info/' + id.value).then(res => {
      userInfoCount.value = res.data
    })
  }
}

// 表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)

// 搜索和筛选条件
const searchForm = reactive({
  keyword: '',
  userId: id.value
})

const activeTab = ref('笔记')

const loadBlog = (isLoadMore) => {
  const currentPageNum = isLoadMore ? pageNum.value + 1 : 1

  let apiUrl = "/blog/front/page"
  if (activeTab.value === '笔记') {
    apiUrl = '/blog/user/page'
  } else if (activeTab.value === '收藏') {
    apiUrl = '/blog/collect/page'
  } else if (activeTab.value === '点赞') {
    apiUrl = '/blog/like/page'
  }

  request.get(apiUrl, {
    params: {
      pageNum: currentPageNum,
      pageSize: pageSize.value,
      userId: searchForm.userId,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (res.code === '200') {
      if (isLoadMore) {
        tableData.value = tableData.value.concat(res.data.records);
        pageNum.value = currentPageNum;
      } else {
        tableData.value = res.data.records;
        pageNum.value = 1;
        waterfallRef.value.init()
      }
      total.value = res.data.total
    }
  })
}

const load = () => {
  loadBlog(false)
}

const loadMore = () => {
  loadBlog(true)
}

const switchTab = (tab) => {
  activeTab.value = tab
  load()
}

const users = ref([])
const loadUsers = () => {
  request.get('/user').then(res => {
    users.value = res.data
  })
}

onMounted(() => {
  load()
  if (id.value) {
    loadUserInfo()
    loadUserInfoCount()
    checkFollow(id.value)
  }
  loadUsers()
  getWaterfallContainerWidth()
})

// 瀑布流组件的引用
const waterfallRef = ref(null)

// 通过ref引用获取dom元素
const waterfallContainerRef = ref(null)
const waterfallContainerWidth = ref(0)

// 获取dom元素宽度
const getWaterfallContainerWidth = () => {
  waterfallContainerWidth.value = waterfallContainerRef.value.clientWidth
}

// 定义列数和博客宽度
const col = ref(4)
const blogWidth = ref(270)

// 计算间距
const gutterWidth = computed(() => {
  const totalBlogWidth = col.value * blogWidth.value
  return Math.floor((waterfallContainerWidth.value - totalBlogWidth) / col.value)
})

// Dialog相关状态
const blog = ref({})
const blogVisible = ref(false)
const dialogStyle = ref({})
const originalPosition = ref({})
const isClosing = ref(false) // 新增：标记是否正在关闭

const showBlog = (item, event) => {

  blog.value = item
  commentItemId.value = item.id
  loadComment()

  checkFollow(blog.value.userId)

  const clickedElement = event.currentTarget
  const rect = clickedElement.getBoundingClientRect()

  // 保存原始位置
  originalPosition.value = {
    top: rect.top,
    left: rect.left,
    width: rect.width,
    height: rect.height
  }

  // 设置初始样式 - 与点击元素完全重合
  dialogStyle.value = {
    position: 'fixed',
    top: `${originalPosition.value.top}px`,
    left: `${originalPosition.value.left}px`,
    width: `${originalPosition.value.width}px`,
    height: `${originalPosition.value.height}px`,
    opacity: '0',
    zIndex: '1800',
    borderRadius: '30px',
  }

  blogVisible.value = true
  isClosing.value = false // 重置关闭状态

  nextTick(() => {
    // 强制重绘
    requestAnimationFrame(() => {
      // 计算目标位置和尺寸
      const targetWidth = Math.min(1200, window.innerWidth * 0.9)
      const targetHeight = Math.min(800, window.innerHeight * 0.9)
      const targetLeft = (window.innerWidth - targetWidth) / 2
      const targetTop = (window.innerHeight - targetHeight) / 2

      // 开始动画到中心位置
      dialogStyle.value = {
        position: 'fixed',
        top: `${targetTop}px`,
        left: `${targetLeft}px`,
        width: `${targetWidth}px`,
        height: `${targetHeight}px`,
        transform: 'scale(1.1)',
        opacity: '1',
        zIndex: '1800',
        borderRadius: '30px',
        transition: 'all 0.5s ease',
      }

    })
  })
}

const closeBlog = () => {

  isClosing.value = true // 设置关闭状态

  // 动画回到原始位置
  dialogStyle.value = {
    position: 'fixed',
    top: `${originalPosition.value.top}px`,
    left: `${originalPosition.value.left}px`,
    width: `${originalPosition.value.width}px`,
    height: `${originalPosition.value.height}px`,
    opacity: '1',
    zIndex: '1800',
    borderRadius: '30px',
    transition: 'all 0.5s ease',
  }

  // 动画完成后隐藏dialog
  setTimeout(() => {
    blogVisible.value = false
    isClosing.value = false
  }, 500)
}

const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})

const commentItemId = ref(0);
const replyVisible = ref(false);
const comments = ref([]);
const commentForm = ref({});

const loadComment = () => {
  request.get("/comment/tree/" + commentItemId.value).then(res => {
    comments.value = res.data;
  });
};

const saveComment = () => {
  if (!account.value.id) {
    ElMessage.warning("请登录后操作");
    return;
  }
  commentForm.value.itemId = commentItemId;
  if (commentForm.value.contentReply) {
    commentForm.value.content = commentForm.value.contentReply;
  }
  request.post("/comment", commentForm.value).then(res => {
    if (res.code === '200') {
      ElMessage.success('评论成功')
      commentForm.value = {};
      loadComment();
    } else {
      ElMessage.error('评论失败')
    }
  })
};

const delComment = (id) => {
  request.delete("/comment/" + id.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功");
      loadComment();
    } else {
      ElMessage.error("删除失败");
    }
  });
};

const handleReply = (pid) => {
  commentForm.value = {pid: pid};
  replyVisible.value = true;
};

const cancelReply = () => {
  commentForm.value = {pid: ''};
  replyVisible.value = false;
};

const isFollowed = ref(false)

const checkFollow = (userId) => {
  request.get('/follow/checkFollow/' + userId).then(res => {
    if (res.code === '200') {
      isFollowed.value = true
    }else {
      isFollowed.value = false
    }
  })
}

const follow = (userId) => {
  let data = {
    itemId: userId,
  }
  request.post("/follow", data).then(res => {
    if (res.code === '200') {
      isFollowed.value = true
      ElMessage.success("关注成功");
    } else {
      isFollowed.value = false
      ElMessage.error(res.msg);
    }
    // 刷新用户统计信息，更新粉丝数
    loadUserInfoCount()
  })
};

// 收藏功能
const collect = (itemId) => {
  let data = {
    itemId: itemId,
  }
  request.post("/collect", data).then(res => {
    if (res.code === '200') {
      blog.value.isCollected = true
      blog.value.collectCount++
      loadBlog()
      ElMessage.success("收藏成功");
    } else {
      blog.value.isCollected = false
      blog.value.collectCount--
      loadBlog()
      ElMessage.error(res.msg);
    }
    // 刷新用户统计信息，更新被收藏量
    loadUserInfoCount()
  })
};

// 点赞功能
const like = (id) => {
  const data = {
    itemId: id,
  }
  request.post("/like", data).then(res => {
    if (res.code === '200') {
      ElMessage.success("点赞成功");
      blog.value.isLiked = true;
      blog.value.likeCount++;
      loadBlog()
    } else {
      ElMessage.error(res.msg || '点赞失败');
      blog.value.isLiked = false;
      blog.value.likeCount--;
      loadBlog()
    }
    // 刷新用户统计信息，更新点赞数
    loadUserInfoCount()
  });
};

const toUser = (userId) =>{
  if (userId) {
    router.push({ path: '/front/user', query: { id: userId } })
  }
}

</script>

<template>
  <div class="user-profile-container">
    <!-- 用户信息头部 -->
    <div class="user-header">
      <div class="user-avatar-section">
        <img :src="userInfo.avatarUrl" :alt="userInfo.nickname" class="user-avatar">
      </div>

      <div class="user-info-section">
        <div class="user-basic-info">
          <h1 class="username">{{ userInfo.nickname }}</h1>
          <div class="user-details">
            <span class="user-id">小红书号：{{ userInfo.id }}</span>
          </div>
        </div>

        <div class="user-bio">
          <p>{{ userInfo.info || '暂无简介哦～' }}</p>
        </div>

        <div class="user-stats">
          <div class="stat-item">
            <span class="stat-number">{{ userInfoCount.followCount }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ userInfoCount.followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ userInfoCount.likeCount }}</span>
            <span class="stat-label">被点赞量</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ userInfoCount.collectCount }}</span>
            <span class="stat-label">被收藏量</span>
          </div>
        </div>
      </div>

      <div class="user-actions">
        <button
            class="follow-btn"
            :class="{ 'followed': isFollowed }"
            @click="follow(userInfo.id)"
            v-if="account.id!=id"
        >
          {{ isFollowed ? '已关注' : '关注' }}
        </button>
      </div>
    </div>

    <!-- 标签导航 -->
    <div class="tab-navigation">
      <div class="tab-item" :class="{ active: activeTab === '笔记' }" @click="switchTab('笔记')">
        笔记
      </div>
      <div class="tab-item" :class="{ active: activeTab === '收藏' }" @click="switchTab('收藏')" v-if="account.id==id || (userInfo.showCollect !== 'false')">
        收藏
      </div>
      <div class="tab-item" :class="{ active: activeTab === '点赞' }" @click="switchTab('点赞')" v-if="account.id==id">
        点赞
      </div>
    </div>

    <!-- 瀑布流内容 -->
    <div ref="waterfallContainerRef">
      <waterfall :data="tableData" :col="col" :width="blogWidth" :gutterWidth="gutterWidth" :loadDistance="30" @loadmore="loadMore" ref="waterfallRef">
        <div class="cell-item" v-for="blog in tableData" :key="blog.id" @click="showBlog(blog,$event)">
          <div class="image-container">
            <img :src="blog.img" alt="加载错误"/>
            <div class="image-overlay"></div>
          </div>
          <div class="item-body">
            <div class="item-desc">{{ blog.name }}</div>
            <div class="item-footer">
              <div class="footer-left">
                <img class="item-img" :src="users.find(user=>user.id===blog.userId)?.avatarUrl" alt="User Avatar"/>
                <div class="name">{{ users.find(user => user.id === blog.userId)?.nickname }}</div>
              </div>
              <div class="footer-right" style="gap: 5px">
                <div style="display: flex">
                  <svg v-if="blog.isLiked" t="1762956264719" class="icon" viewBox="0 0 1024 1024" version="1.1"
                       xmlns="http://www.w3.org/2000/svg" p-id="37873" width="14" height="14">
                    <path
                        d="M64 483.04V872c0 37.216 30.144 67.36 67.36 67.36H192V416.32l-60.64-0.64A67.36 67.36 0 0 0 64 483.04zM857.28 344.992l-267.808 1.696c12.576-44.256 18.944-83.584 18.944-118.208 0-78.56-68.832-155.488-137.568-145.504-60.608 8.8-67.264 61.184-67.264 126.816v59.264c0 76.064-63.84 140.864-137.856 148L256 416.96v522.4h527.552a102.72 102.72 0 0 0 100.928-83.584l73.728-388.96a102.72 102.72 0 0 0-100.928-121.824z"
                        p-id="37874" fill="#d81e06"></path>
                  </svg>
                  <svg v-else t="1762956264719" class="icon" viewBox="0 0 1024 1024" version="1.1"
                       xmlns="http://www.w3.org/2000/svg" p-id="37873" width="14" height="14">
                    <path
                        d="M64 483.04V872c0 37.216 30.144 67.36 67.36 67.36H192V416.32l-60.64-0.64A67.36 67.36 0 0 0 64 483.04zM857.28 344.992l-267.808 1.696c12.576-44.256 18.944-83.584 18.944-118.208 0-78.56-68.832-155.488-137.568-145.504-60.608 8.8-67.264 61.184-67.264 126.816v59.264c0 76.064-63.84 140.864-137.856 148L256 416.96v522.4h527.552a102.72 102.72 0 0 0 100.928-83.584l73.728-388.96a102.72 102.72 0 0 0-100.928-121.824z"
                        p-id="37874" fill="#e6e6e6"></path>
                  </svg>
                  <span>{{ blog.likeCount || 0 }}</span>
                </div>
                <div>
                  <svg v-if="blog.isCollected" t="1762955885787" class="icon" viewBox="0 0 1024 1024" version="1.1"
                       xmlns="http://www.w3.org/2000/svg" p-id="36882" id="mx_n_1762955885787" width="14" height="14">
                    <path
                        d="M575.12 131l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33l-153.3 149.39a47.68 47.68 0 0 0-13.73 42.2l36.19 211c6.68 39-34.2 68.65-69.18 50.26l-189.46-99.62a47.68 47.68 0 0 0-44.38 0L320.7 914c-35 18.39-75.86-11.31-69.18-50.26l36.19-211A47.68 47.68 0 0 0 274 610.58L120.7 461.16c-28.3-27.58-12.68-75.65 26.43-81.33L359 349.05A47.68 47.68 0 0 0 394.87 323l94.73-192c17.49-35.43 68.03-35.43 85.52 0z"
                        fill="#FED547" p-id="36883"></path>
                    <path
                        d="M943.3 461.77c28.3-27.58 12.68-75.65-26.43-81.33L705 349.66a47.68 47.68 0 0 1-35.9-26.08l-94.74-192c-16.31-33.05-61.35-35.25-81.53-6.65a49.51 49.51 0 0 1 4 6.65l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33L712.47 611.19a47.68 47.68 0 0 0-13.71 42.2l36.19 211a46.76 46.76 0 0 1-11.52 39.81l19.89 10.46c35 18.39 75.86-11.31 69.18-50.26l-36.19-211A47.68 47.68 0 0 1 790 611.19z"
                        fill="#E2B742" p-id="36884"></path>
                    <path
                        d="M263.93 925.77a54 54 0 0 1-53.06-63.05l36.19-211A41.47 41.47 0 0 0 235.13 615L81.85 465.6a53.88 53.88 0 0 1 29.87-91.9l211.83-30.78a41.51 41.51 0 0 0 31.23-22.69l94.73-192a53.89 53.89 0 0 1 96.64 0l94.74 192a41.5 41.5 0 0 0 31.23 22.69L884 373.7a53.88 53.88 0 0 1 29.86 91.9L760.53 615a41.45 41.45 0 0 0-11.93 36.71l36.19 211a53.88 53.88 0 0 1-78.18 56.8l-189.48-99.6a41.49 41.49 0 0 0-38.6 0l-189.47 99.61a53.86 53.86 0 0 1-25.13 6.25z m233.9-815.13a40.71 40.71 0 0 0-37.19 23.12l-94.73 192a53.89 53.89 0 0 1-40.57 29.47L113.5 386a41.48 41.48 0 0 0-23 70.75l153.3 149.39a53.88 53.88 0 0 1 15.5 47.69l-36.19 211a41.48 41.48 0 0 0 60.18 43.73l189.48-99.61a53.81 53.81 0 0 1 50.15 0l189.47 99.61a41.48 41.48 0 0 0 60.18-43.73l-36.19-211a53.86 53.86 0 0 1 15.5-47.69l153.28-149.42a41.48 41.48 0 0 0-23-70.75l-211.82-30.78a53.88 53.88 0 0 1-40.57-29.47L535 133.75a40.71 40.71 0 0 0-37.17-23.12z"
                        fill="#28CA67" p-id="36885"></path>
                  </svg>
                  <svg v-else t="1762955885787" class="icon" viewBox="0 0 1024 1024" version="1.1"
                       xmlns="http://www.w3.org/2000/svg" p-id="36882" id="mx_n_1762955885787" width="14" height="14">
                    <path
                        d="M575.12 131l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33l-153.3 149.39a47.68 47.68 0 0 0-13.73 42.2l36.19 211c6.68 39-34.2 68.65-69.18 50.26l-189.46-99.62a47.68 47.68 0 0 0-44.38 0L320.7 914c-35 18.39-75.86-11.31-69.18-50.26l36.19-211A47.68 47.68 0 0 0 274 610.58L120.7 461.16c-28.3-27.58-12.68-75.65 26.43-81.33L359 349.05A47.68 47.68 0 0 0 394.87 323l94.73-192c17.49-35.43 68.03-35.43 85.52 0z"
                        fill="#e6e6e6" p-id="36883"></path>
                    <path
                        d="M943.3 461.77c28.3-27.58 12.68-75.65-26.43-81.33L705 349.66a47.68 47.68 0 0 1-35.9-26.08l-94.74-192c-16.31-33.05-61.35-35.25-81.53-6.65a49.51 49.51 0 0 1 4 6.65l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33L712.47 611.19a47.68 47.68 0 0 0-13.71 42.2l36.19 211a46.76 46.76 0 0 1-11.52 39.81l19.89 10.46c35 18.39 75.86-11.31 69.18-50.26l-36.19-211A47.68 47.68 0 0 1 790 611.19z"
                        fill="#e6e6e6" p-id="36884"></path>
                    <path
                        d="M263.93 925.77a54 54 0 0 1-53.06-63.05l36.19-211A41.47 41.47 0 0 0 235.13 615L81.85 465.6a53.88 53.88 0 0 1 29.87-91.9l211.83-30.78a41.51 41.51 0 0 0 31.23-22.69l94.73-192a53.89 53.89 0 0 1 96.64 0l94.74 192a41.5 41.5 0 0 0 31.23 22.69L884 373.7a53.88 53.88 0 0 1 29.86 91.9L760.53 615a41.45 41.45 0 0 0-11.93 36.71l36.19 211a53.88 53.88 0 0 1-78.18 56.8l-189.48-99.6a41.49 41.49 0 0 0-38.6 0l-189.47 99.61a53.86 53.86 0 0 1-25.13 6.25z m233.9-815.13a40.71 40.71 0 0 0-37.19 23.12l-94.73 192a53.89 53.89 0 0 1-40.57 29.47L113.5 386a41.48 41.48 0 0 0-23 70.75l153.3 149.39a53.88 53.88 0 0 1 15.5 47.69l-36.19 211a41.48 41.48 0 0 0 60.18 43.73l189.48-99.61a53.81 53.81 0 0 1 50.15 0l189.47 99.61a41.48 41.48 0 0 0 60.18-43.73l-36.19-211a53.86 53.86 0 0 1 15.5-47.69l153.28-149.42a41.48 41.48 0 0 0-23-70.75l-211.82-30.78a53.88 53.88 0 0 1-40.57-29.47L535 133.75a40.71 40.71 0 0 0-37.17-23.12z"
                        fill="#e6e6e6" p-id="36885"></path>
                  </svg>
                  <span>{{ blog.collectCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </waterfall>
    </div>

    <!-- 背景遮罩 -->
    <div v-if="blogVisible" class="dialog-overlay" @click="closeBlog"></div>

    <!-- 自定义Dialog -->
    <div v-if="blogVisible" class="custom-dialog-container" :style="dialogStyle">
      <!-- 关闭按钮在左上角，关闭时隐藏 -->
      <button v-show="!isClosing" @click="closeBlog" class="close-button">
        <i class="el-icon-close">×</i>
      </button>

      <div class="custom-dialog-content">
        <div class="img-video-container" :class="{ 'closing-animation': isClosing }">
          <video controls autoplay :src="blog.video" class="video-player" v-if="blog.category==='视频'"></video>
          <el-image :src="blog.img" class="img" :preview-src-list="[blog.img]" v-else></el-image>
        </div>

        <!-- 右侧内容在关闭时隐藏 -->
        <div class="blog-container" :class="{ 'closing-hide': isClosing }">
          <!-- 右侧内容区域，使用滚动条 -->
          <div class="content-scroll-area">
            <!-- 用户信息和关注按钮 -->
            <div class="user-info">
              <img :src="users.find(user=>user.id===blog.userId)?.avatarUrl" alt="User Avatar" class="user-avatar" @click="toUser(blog.userId)" style="cursor: pointer">
              <span class="user-name" @click="toUser(blog.userId)">{{ users.find(user => user.id === blog.userId)?.nickname }}</span>
              <button
                  class="follow-button"
                  :class="{ 'follow-button--followed': isFollowed }"
                  @click="follow(blog.userId)"
              >
                {{ isFollowed ? '已关注' : '关注' }}
              </button>
            </div>

            <div class="blog-content">
              <h2 class="blog-title">{{ blog.name }}</h2>
              <p class="blog-description" v-html="blog.content"></p>
              <span class="blog-time">{{ blog.time }}</span>
            </div>

            <el-divider></el-divider>

            <!-- 评论区域 -->
            <div class="comment-section">
              <div class="comment-header">
                <span class="comment-count">共 {{ comments.length }} 条评论</span>
              </div>

              <div class="comment-list">
                <div v-for="item in comments" :key="item.id" class="comment-thread">
                  <div class="comment-item">
                    <div class="comment-avatar">
                      <el-image :src="item.avatarUrl"></el-image>
                    </div>
                    <div class="comment-content">
                      <div class="comment-user">{{ item.nickname }}</div>
                      <div class="comment-text">{{ item.content }}</div>
                      <div class="comment-meta">
                        <span class="comment-time">{{ item.time }}</span>
                        <el-button link @click="handleReply(item.id)" class="comment-reply-btn">回复</el-button>
                        <el-button
                            link
                            @click="delComment(item.id)"
                            v-if="account.id === item.userId || account.role === 'ROLE_ADMIN'"
                            class="comment-delete-btn"
                        >
                          删除
                        </el-button>
                      </div>
                      <div class="comment-reply" v-if="commentForm.pid === item.id && replyVisible">
                        <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..."
                                  size="small"></el-input>
                        <div class="reply-actions">
                          <el-button size="small" type="primary" @click="saveComment">发布</el-button>
                          <el-button size="small" @click="cancelReply">取消</el-button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 子评论 -->
                  <template v-if="item.children?.length">
                    <div v-for="subItem in item.children" class="comment-item comment-sub-item" :key="subItem.id">
                      <div class="comment-avatar">
                        <el-image :src="subItem.avatarUrl"/>
                      </div>
                      <div class="comment-content">
                        <div class="comment-user">
                          {{ subItem.nickname }}
                          <span v-if="subItem.pid" class="reply-target">回复 @{{ subItem.pnickname }}</span>
                        </div>
                        <div class="comment-text">{{ subItem.content }}</div>
                        <div class="comment-meta">
                          <span class="comment-time">{{ subItem.time }}</span>
                          <el-button link @click="handleReply(subItem.id)" class="comment-reply-btn">回复</el-button>
                          <el-button
                              link
                              @click="delComment(subItem.id)"
                              v-if="account.id === subItem.userId || account.role === 'ROLE_ADMIN'"
                              class="comment-delete-btn"
                          >
                            删除
                          </el-button>
                        </div>
                        <div class="comment-reply" v-if="commentForm.pid === subItem.id && replyVisible">
                          <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..."
                                    size="small"></el-input>
                          <div class="reply-actions">
                            <el-button size="small" type="primary" @click="saveComment">发布</el-button>
                            <el-button size="small" @click="cancelReply">取消</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- 底部留白，为固定评论框留出空间 -->
              <div class="comments-bottom-spacer"></div>
            </div>
          </div>

          <!-- 固定在底部的互动区域 -->
          <div class="interaction-footer">
            <div class="interaction-stats">
              <span class="stat-item" @click="like(blog.id)">
                    <svg v-if="blog.isLiked" t="1762956264719" class="icon" viewBox="0 0 1024 1024" version="1.1"
                         xmlns="http://www.w3.org/2000/svg" p-id="37873" width="14" height="14">
                      <path
                          d="M64 483.04V872c0 37.216 30.144 67.36 67.36 67.36H192V416.32l-60.64-0.64A67.36 67.36 0 0 0 64 483.04zM857.28 344.992l-267.808 1.696c12.576-44.256 18.944-83.584 18.944-118.208 0-78.56-68.832-155.488-137.568-145.504-60.608 8.8-67.264 61.184-67.264 126.816v59.264c0 76.064-63.84 140.864-137.856 148L256 416.96v522.4h527.552a102.72 102.72 0 0 0 100.928-83.584l73.728-388.96a102.72 102.72 0 0 0-100.928-121.824z"
                          p-id="37874" fill="#d81e06"></path>
                    </svg>
                    <svg v-else t="1762956264719" class="icon" viewBox="0 0 1024 1024" version="1.1"
                         xmlns="http://www.w3.org/2000/svg" p-id="37873" width="14" height="14">
                      <path
                          d="M64 483.04V872c0 37.216 30.144 67.36 67.36 67.36H192V416.32l-60.64-0.64A67.36 67.36 0 0 0 64 483.04zM857.28 344.992l-267.808 1.696c12.576-44.256 18.944-83.584 18.944-118.208 0-78.56-68.832-155.488-137.568-145.504-60.608 8.8-67.264 61.184-67.264 126.816v59.264c0 76.064-63.84 140.864-137.856 148L256 416.96v522.4h527.552a102.72 102.72 0 0 0 100.928-83.584l73.728-388.96a102.72 102.72 0 0 0-100.928-121.824z"
                          p-id="37874" fill="#e6e6e6"></path>
                    </svg>
                  <span>{{ blog.likeCount || 0 }}</span>
              </span>
              <span class="stat-item" @click="collect(blog.id)">
                    <svg v-if="blog.isCollected" t="1762955885787" class="icon" viewBox="0 0 1024 1024" version="1.1"
                         xmlns="http://www.w3.org/2000/svg" p-id="36882" id="mx_n_1762955885787" width="14" height="14">
                      <path
                          d="M575.12 131l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33l-153.3 149.39a47.68 47.68 0 0 0-13.73 42.2l36.19 211c6.68 39-34.2 68.65-69.18 50.26l-189.46-99.62a47.68 47.68 0 0 0-44.38 0L320.7 914c-35 18.39-75.86-11.31-69.18-50.26l36.19-211A47.68 47.68 0 0 0 274 610.58L120.7 461.16c-28.3-27.58-12.68-75.65 26.43-81.33L359 349.05A47.68 47.68 0 0 0 394.87 323l94.73-192c17.49-35.43 68.03-35.43 85.52 0z"
                          fill="#FED547" p-id="36883"></path>
                      <path
                          d="M943.3 461.77c28.3-27.58 12.68-75.65-26.43-81.33L705 349.66a47.68 47.68 0 0 1-35.9-26.08l-94.74-192c-16.31-33.05-61.35-35.25-81.53-6.65a49.51 49.51 0 0 1 4 6.65l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33L712.47 611.19a47.68 47.68 0 0 0-13.71 42.2l36.19 211a46.76 46.76 0 0 1-11.52 39.81l19.89 10.46c35 18.39 75.86-11.31 69.18-50.26l-36.19-211A47.68 47.68 0 0 1 790 611.19z"
                          fill="#E2B742" p-id="36884"></path>
                      <path
                          d="M263.93 925.77a54 54 0 0 1-53.06-63.05l36.19-211A41.47 41.47 0 0 0 235.13 615L81.85 465.6a53.88 53.88 0 0 1 29.87-91.9l211.83-30.78a41.51 41.51 0 0 0 31.23-22.69l94.73-192a53.89 53.89 0 0 1 96.64 0l94.74 192a41.5 41.5 0 0 0 31.23 22.69L884 373.7a53.88 53.88 0 0 1 29.86 91.9L760.53 615a41.45 41.45 0 0 0-11.93 36.71l36.19 211a53.88 53.88 0 0 1-78.18 56.8l-189.48-99.6a41.49 41.49 0 0 0-38.6 0l-189.47 99.61a53.86 53.86 0 0 1-25.13 6.25z m233.9-815.13a40.71 40.71 0 0 0-37.19 23.12l-94.73 192a53.89 53.89 0 0 1-40.57 29.47L113.5 386a41.48 41.48 0 0 0-23 70.75l153.3 149.39a53.88 53.88 0 0 1 15.5 47.69l-36.19 211a41.48 41.48 0 0 0 60.18 43.73l189.48-99.61a53.81 53.81 0 0 1 50.15 0l189.47 99.61a41.48 41.48 0 0 0 60.18-43.73l-36.19-211a53.86 53.86 0 0 1 15.5-47.69l153.28-149.42a41.48 41.48 0 0 0-23-70.75l-211.82-30.78a53.88 53.88 0 0 1-40.57-29.47L535 133.75a40.71 40.71 0 0 0-37.17-23.12z"
                          fill="#28CA67" p-id="36885"></path>
                    </svg>
                    <svg v-else t="1762955885787" class="icon" viewBox="0 0 1024 1024" version="1.1"
                         xmlns="http://www.w3.org/2000/svg" p-id="36882" id="mx_n_1762955885787" width="14" height="14">
                      <path
                          d="M575.12 131l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33l-153.3 149.39a47.68 47.68 0 0 0-13.73 42.2l36.19 211c6.68 39-34.2 68.65-69.18 50.26l-189.46-99.62a47.68 47.68 0 0 0-44.38 0L320.7 914c-35 18.39-75.86-11.31-69.18-50.26l36.19-211A47.68 47.68 0 0 0 274 610.58L120.7 461.16c-28.3-27.58-12.68-75.65 26.43-81.33L359 349.05A47.68 47.68 0 0 0 394.87 323l94.73-192c17.49-35.43 68.03-35.43 85.52 0z"
                          fill="#e6e6e6" p-id="36883"></path>
                      <path
                          d="M943.3 461.77c28.3-27.58 12.68-75.65-26.43-81.33L705 349.66a47.68 47.68 0 0 1-35.9-26.08l-94.74-192c-16.31-33.05-61.35-35.25-81.53-6.65a49.51 49.51 0 0 1 4 6.65l94.74 192a47.68 47.68 0 0 0 35.9 26.08l211.84 30.78c39.11 5.68 54.72 53.74 26.43 81.33L712.47 611.19a47.68 47.68 0 0 0-13.71 42.2l36.19 211a46.76 46.76 0 0 1-11.52 39.81l19.89 10.46c35 18.39 75.86-11.31 69.18-50.26l-36.19-211A47.68 47.68 0 0 1 790 611.19z"
                          fill="#e6e6e6" p-id="36884"></path>
                      <path
                          d="M263.93 925.77a54 54 0 0 1-53.06-63.05l36.19-211A41.47 41.47 0 0 0 235.13 615L81.85 465.6a53.88 53.88 0 0 1 29.87-91.9l211.83-30.78a41.51 41.51 0 0 0 31.23-22.69l94.73-192a53.89 53.89 0 0 1 96.64 0l94.74 192a41.5 41.5 0 0 0 31.23 22.69L884 373.7a53.88 53.88 0 0 1 29.86 91.9L760.53 615a41.45 41.45 0 0 0-11.93 36.71l36.19 211a53.88 53.88 0 0 1-78.18 56.8l-189.48-99.6a41.49 41.49 0 0 0-38.6 0l-189.47 99.61a53.86 53.86 0 0 1-25.13 6.25z m233.9-815.13a40.71 40.71 0 0 0-37.19 23.12l-94.73 192a53.89 53.89 0 0 1-40.57 29.47L113.5 386a41.48 41.48 0 0 0-23 70.75l153.3 149.39a53.88 53.88 0 0 1 15.5 47.69l-36.19 211a41.48 41.48 0 0 0 60.18 43.73l189.48-99.61a53.81 53.81 0 0 1 50.15 0l189.47 99.61a41.48 41.48 0 0 0 60.18-43.73l-36.19-211a53.86 53.86 0 0 1 15.5-47.69l153.28-149.42a41.48 41.48 0 0 0-23-70.75l-211.82-30.78a53.88 53.88 0 0 1-40.57-29.47L535 133.75a40.71 40.71 0 0 0-37.17-23.12z"
                          fill="#e6e6e6" p-id="36885"></path>
                    </svg>
                <span>{{ blog.collectCount || 0 }}</span>
              </span>
            </div>

            <!-- 评论输入框 -->
            <div class="comment-input-wrapper">
              <el-input
                  class="comment-input"
                  v-model="commentForm.content"
                  placeholder="说点什么..."
                  size="small"
              />
              <el-button
                  :disabled="!commentForm.content"
                  class="comment-send-btn"
                  type="primary"
                  size="small"
                  @click="saveComment"
              >
                发送
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped lang="scss">
.user-profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.user-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 40px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 30px;

  .user-avatar-section {
    flex-shrink: 0;

    .user-avatar {
      width: 120px;
      height: 120px;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid #fff;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    }
  }

  .user-info-section {
    flex: 1;
    min-width: 0;

    .user-basic-info {
      margin-bottom: 16px;

      .username {
        font-size: 28px;
        font-weight: 600;
        color: #333;
        margin: 0 0 8px 0;
      }

      .user-details {
        display: flex;
        gap: 16px;
        font-size: 14px;
        color: #666;

        .user-id {
          display: flex;
          align-items: center;
        }
      }
    }

    .user-bio {
      margin-bottom: 20px;

      p {
        margin: 4px 0;
        font-size: 14px;
        line-height: 1.5;
        color: #333;
      }
    }

    .user-stats {
      display: flex;
      gap: 32px;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .stat-number {
          font-size: 18px;
          font-weight: 600;
          color: #333;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          color: #666;
        }

      }
    }
  }

  .user-actions {
    display: flex;
    gap: 12px;
    align-items: flex-start;

    .follow-btn {
      background-color: #ff2442;
      color: #fff;
      border: none;
      padding: 10px 24px;
      border-radius: 20px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background-color: #e01e3c;
      }

      &.followed {
        background-color: #f0f0f0;
        color: #666;
        border: 1px solid #d9d9d9;

        &:hover {
          background-color: #e8e8e8;
        }
      }
    }

  }
}

.tab-navigation {
  display: flex;
  justify-content: center;
  gap: 60px;
  margin-bottom: 30px;
  border-bottom: 1px solid #f0f0f0;

  .tab-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 16px 0;
    font-size: 16px;
    color: #666;
    cursor: pointer;
    position: relative;
    transition: color 0.2s ease;

    &:hover {
      color: #333;
    }

    &.active {
      color: #333;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: -1px;
        left: 50%;
        transform: translateX(-50%);
        width: 24px;
        height: 2px;
        background-color: #333;
        border-radius: 1px;
      }
    }
  }
}

.cell-item {
  width: 100%;
  height: auto;
  margin-bottom: 6px;
  background: #ffffff;
  border-radius: 6px;
  overflow: hidden;
  box-sizing: border-box;
  cursor: pointer;

  .image-container {
    position: relative;
    overflow: hidden;
    border-radius: 20px;

    img {
      width: 100%;
      height: auto;
      display: block;
      transition: none;
    }

    .image-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.3);
      opacity: 0;
      transition: opacity 0.3s ease;
      pointer-events: none;
    }
  }

  &:hover .image-overlay {
    opacity: 1;
  }

  .item-body {
    margin: 9px;

    .item-desc {
      text-align: left;
      font-family: Roboto;
      font-style: normal;
      font-weight: normal;
      font-size: 16px;
      line-height: 16px;
      color: #000000;
      margin-left: 5px;
    }

    .item-footer {
      display: flex;
      justify-content: space-between;

      .footer-left {
        display: flex;
        align-items: center;
        font-family: SF Pro Display;
        font-style: normal;
        font-weight: normal;
        font-size: 12px;
        line-height: 14px;
        margin-top: 7px;
        color: rgba(0, 0, 0, 0.6);

        .item-img {
          border-radius: 50%;
          width: 22px;
          height: 22px;
          margin-right: 4px;
        }
      }

      .footer-right {
        font-size: 14px;
        display: flex;
        align-items: center;

        .icon {
          margin-right: 4px;
          cursor: pointer;
        }
      }
    }
  }
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 1800;
}

.custom-dialog-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  will-change: transform, opacity;

  .close-button {
    position: absolute;
    top: 15px;
    left: 15px;
    background: rgba(0, 0, 0, 0.6);
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    font-size: 18px;
    cursor: pointer;
    z-index: 10;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.8);
    }
  }

  .custom-dialog-content {
    display: flex;
    width: 100%;
    height: 100%;

    .img-video-container {
      flex: 1;
      background-color: #000;
      display: flex;
      align-items: center;
      justify-content: center;

      &.closing-animation {
        flex: 1;
        width: 100%;
      }

      .video-player {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      .img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }

    .blog-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      position: relative;
      background: #fff;
      transition: all 0.5s ease;

      &.closing-hide {
        width: 0;
        flex: 0;
      }
    }
  }
}

.content-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px;
  padding-bottom: 120px; // 为固定底部区域留出空间

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;

    &:hover {
      background: #a8a8a8;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  padding: 20px 0 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;

  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 12px;
  }

  .user-name {
    cursor: pointer;
    font-weight: 600;
    font-size: 16px;
    color: #333;
    flex: 1;
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

      &:hover {
        background-color: #e8e8e8;
      }
    }
  }
}

.blog-content {
  .blog-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
    line-height: 1.4;
  }

  .blog-description {
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    margin-bottom: 12px;
    word-break: break-word;
  }

  .blog-time {
    font-size: 12px;
    color: #999;
    display: block;
    margin-bottom: 16px;
  }
}

.comment-section {
  margin-top: 20px;
}

.comment-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;

  .comment-count {
    font-size: 14px;
    color: #666;
  }
}

.comment-list {
  .comment-thread {
    margin-bottom: 16px;
  }
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;

  &.comment-sub-item {
    margin-left: 40px;
    padding: 12px;
    background-color: #f8f8f8;
    border-radius: 8px;
  }

  .comment-avatar {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .comment-content {
    flex: 1;
    min-width: 0;
  }

  .comment-user {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;

    .reply-target {
      color: #1890ff;
      font-weight: normal;
    }
  }

  .comment-text {
    font-size: 14px;
    line-height: 1.4;
    color: #333;
    margin-bottom: 8px;
    word-break: break-word;
  }

  .comment-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #999;

    .comment-time {
      color: #999;
    }

    .comment-reply-btn,
    .comment-delete-btn {
      font-size: 12px;
      color: #666;
      padding: 0;
      height: auto;

      &:hover {
        color: #1890ff;
      }
    }

    .comment-delete-btn:hover {
      color: #ff4d4f;
    }
  }

  .comment-reply {
    margin-top: 12px;
    padding: 12px;
    background-color: #f5f5f5;
    border-radius: 6px;

    .reply-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      margin-top: 8px;
    }
  }
}

.comments-bottom-spacer {
  height: 20px;
}

.interaction-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;

  .interaction-stats {
    display: flex;
    gap: 20px;
    margin-bottom: 12px;

    .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
        cursor: pointer;
        color: #666;
        font-size: 14px;
        transition: color 0.2s ease;

        &:hover {
          color: #ff2442;
        }

        .icon {
          margin-right: 4px;
          cursor: pointer;
        }
      }
  }

  .comment-input-wrapper {
    display: flex;
    gap: 8px;
    align-items: flex-end;

    .comment-input {
      flex: 1;

      :deep(.el-input__wrapper) {
        border-radius: 20px;
        background-color: #f8f8f8;
        border: 1px solid #e0e0e0;

        &.is-focus {
          border-color: #1890ff;
          background-color: #fff;
        }
      }
    }

    .comment-send-btn {
      border-radius: 16px;
      padding: 8px 16px;
    }
  }
}
</style>