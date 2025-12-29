<script setup>
import {ref, onMounted, reactive, computed, nextTick, watch} from "vue";
import request from "@/utils/request.js";
import { Star, StarFilled } from '@element-plus/icons-vue';
import { useRouter, useRoute } from 'vue-router';

const route = useRoute();
const router = useRouter();
import {ElMessage} from "element-plus";

// 响应式列数调整
const updateColumns = () => {
  const width = window.innerWidth;
  if (width < 480) {
    col.value = 1;
    blogWidth.value = width - 40;
  } else if (width < 768) {
    col.value = 2;
    blogWidth.value = 200;
  } else if (width < 1024) {
    col.value = 3;
    blogWidth.value = 220;
  } else if (width < 1280) {
    col.value = 4;
    blogWidth.value = 240;
  } else {
    col.value = 5;
    blogWidth.value = 250;
  }
  getWaterfallContainerWidth();
};

// 监听窗口大小变化
if (typeof window !== 'undefined') {
  window.addEventListener('resize', updateColumns);
}

// 从路由参数获取初始搜索关键词
const keyword = ref(route.query.keyword || '')

// 监听路由参数变化，当关键词变化时重新加载搜索结果
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword !== keyword.value) {
    keyword.value = newKeyword || '';
    searchForm.keyword = keyword.value;
    load();
  }
});

const types = ref([])
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data
    types.value.unshift({
      id: 0,
      name: '全部'
    })
  })
}

onMounted(() => {
  loadType()
  updateColumns()
  getWaterfallContainerWidth()
  loadUsers()
  // 初始化时将路由参数的关键词赋值给搜索表单
  searchForm.keyword = keyword.value
  load(false)
})

const searchForm = reactive({
  typeId: 0,
  keyword: ''
})

//表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(15)

// 加载数据
const loadBlog = (loadMore) => {
  const currentPageNum = loadMore ? pageNum.value + 1 : 1
  request.get("/blog/front/page", {
    params: {
      pageNum: currentPageNum,
      pageSize: pageSize.value,
      typeId: searchForm.typeId,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (loadMore) {
      tableData.value = tableData.value.concat(res.data.records)
      pageNum.value = currentPageNum
    } else {
      pageNum.value = 1
      tableData.value = res.data.records
      waterfallRef.value.init()
    }
    pageNum.value = currentPageNum
    total.value = res.data.total
  })
}

const load = () => {
  loadBlog(false)
}

const loadMore = () => {
  loadBlog(true)
}

const users = ref([])
const loadUsers = () => {
  request.get('/user').then(res => {
    if (res.data) {
      users.value = res.data
    }
  })
}

//通过ref引用获取dom元素
const waterfallContainerRef = ref(null)
const waterfallContainerWidth = ref(0)

//获取dom元素宽度
const getWaterfallContainerWidth = () => {
  // 加?.避免DOM未挂载时的错误
  waterfallContainerWidth.value = waterfallContainerRef.value?.clientWidth || 0
}
//定义列数和博客宽度
const col = ref(5)
const blogWidth = ref(250)
//计算间距
const gutterWidth = computed(() => {
  const totalBlogWidth = col.value * blogWidth.value
  const remainingSpace = waterfallContainerWidth.value - totalBlogWidth
  // 间距 = 剩余空间 / (列数 + 1)，确保左右两侧也有间距
  // 如果剩余空间不足，使用最小间距10px
  const calculatedGutter = Math.floor(remainingSpace / (col.value + 1))
  return Math.max(10, calculatedGutter)
})

const waterfallRef = ref(null)

const blog = ref({})
const blogVisible = ref(false)
const dialogStyle = ref({})
const originalPosition = ref({})
const isClosing = ref(false)

// 多图相关
const blogImages = ref([])
const currentImageIndex = ref(0)

const showBlog = (item, event) => {

  blog.value = item
  commentItemId.value = item.id
  checkFollow(blog.value.userId)
  loadComment()
  
  // 加载多图数据
  blogImages.value = []
  currentImageIndex.value = 0
  
  // 优先使用封面图作为第一张
  if (item.img) {
    blogImages.value.push(item.img)
  }
  
  // 如果有多图数据，添加到数组中（排除封面图重复）
  if (item.images) {
    try {
      const imagesArray = JSON.parse(item.images)
      // 过滤掉与封面图相同的图片，避免重复
      const filteredImages = imagesArray.filter(img => img !== item.img)
      blogImages.value = blogImages.value.concat(filteredImages)
    } catch (e) {
      console.error('解析多图数据失败:', e)
    }
  }

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

// 图片切换
const prevImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
  }
}

const nextImage = () => {
  if (currentImageIndex.value < blogImages.value.length - 1) {
    currentImageIndex.value++
  }
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
  commentForm.value.itemId = commentItemId.value;
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
  request.delete("/comment/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功");
      loadComment();
    } else {
      ElMessage.error("删除失败");
    }
  });
};

const handleReply = (pid) => {
  commentForm.value = { pid: pid };
  replyVisible.value = true;
};

const cancelReply = () => {
  commentForm.value = { pid: '' };
  replyVisible.value = false;
};

const collect = (id) => {
  const data = {
    itemId: id,
  }
  request.post("/collect", data).then(res => {
    if (res.code === '200') {
      ElMessage.success("收藏成功");
      blog.value.isCollected = true;
      blog.value.count++;
      loadBlog()
    } else {
      ElMessage.error(res.msg || '收藏失败');
      blog.value.isCollected = false;
      blog.value.count--;
      loadBlog()
    }
  });
};

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
  });
};

const isFollowed = ref(false)

const checkFollow = (id) => {
  request.get("/follow/checkFollow/" + id).then(res => {
    if (res.code === '200') {
      isFollowed.value = true
    } else {
      isFollowed.value = false
    }
  });
};

const follow = (id) => {
  request.post("/follow", {
    itemId: id
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success("关注成功");
      isFollowed.value = true;
    } else {
      ElMessage.error(res.msg || '关注失败');
      isFollowed.value = false;
    }
  });
};

</script>

<template>
  <!-- 搜索关键词显示区域 -->
  <div class="search-keyword-display">
    <h2 class="search-title">搜索结果：{{ keyword || '无' }}</h2>
    <p class="search-subtitle">找到 {{ total }} 个相关内容</p>
  </div>

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
              <div class="name">{{ users.find(user=>user.id===blog.userId)?.nickname }}</div>
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
                <span style="color: #333333cc">{{ blog.likeCount || 0 }}</span>
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
                <span style="color: #333333cc">{{ blog.collectCount || 0 }}</span>
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
      x
    </button>

    <div class="custom-dialog-content">
      <div class="img-video-container" :class="{ 'closing-animation': isClosing }">
        <video controls autoplay :src="blog.video" class="video-player" v-if="blog.category==='视频'"></video>
        <div v-else class="image-carousel">
          <!-- 图片容器 -->
          <div class="carousel-main">
            <el-image 
              :src="blogImages[currentImageIndex]" 
              class="img" 
              :preview-src-list="blogImages" 
              :initial-index="currentImageIndex"
              fit="contain"
            ></el-image>
          </div>
          
          <!-- 切换按钮 -->
          <button 
            v-if="blogImages.length > 1 && currentImageIndex > 0" 
            @click.stop="prevImage" 
            class="carousel-btn carousel-btn-prev"
          >
            ‹
          </button>
          <button 
            v-if="blogImages.length > 1 && currentImageIndex < blogImages.length - 1" 
            @click.stop="nextImage" 
            class="carousel-btn carousel-btn-next"
          >
            ›
          </button>
          
          <!-- 指示器 -->
          <div v-if="blogImages.length > 1" class="carousel-indicators">
            <span 
              v-for="(img, index) in blogImages" 
              :key="index" 
              :class="['indicator', { 'active': index === currentImageIndex }]"
              @click.stop="currentImageIndex = index"
            ></span>
          </div>
        </div>
      </div>

      <!-- 右侧内容在关闭时隐藏 -->
      <div class="blog-container" :class="{ 'closing-hide': isClosing }">
        <!-- 右侧内容区域，使用滚动条 -->
        <div class="content-scroll-area">
          <!-- 用户信息和关注按钮 -->
          <div class="user-info">
            <img :src="users.find(user=>user.id===blog.userId)?.avatarUrl" alt="User Avatar" class="user-avatar">
            <span class="user-name" @click="router.push('/front/user?id='+blog.userId)">{{ users.find(user=>user.id===blog.userId)?.nickname }}</span>
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
                  <div class="comment-avatar" @click="router.push('/front/user?id=' + item.userId)">
                    <el-image :src="item.avatarUrl"></el-image>
                  </div>
                  <div class="comment-content">
                    <div class="comment-user" @click="router.push('/front/user?id=' + item.userId)" style="cursor: pointer;">{{ item.nickname }}</div>
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
                      <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..." size="small"></el-input>
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
                    <div class="comment-avatar" @click="router.push('/front/user?id=' + subItem.userId)">
                      <el-image :src="subItem.avatarUrl" />
                    </div>
                    <div class="comment-content">
                      <div class="comment-user">
                        <span @click="router.push('/front/user?id=' + subItem.userId)" style="cursor: pointer;">{{ subItem.nickname }}</span>
                        <span v-if="subItem.pid" class="reply-target">回复 @<span @click="router.push('/front/user?id=' + subItem.pUserId)" style="cursor: pointer;">{{ subItem.pnickname }}</span></span>
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
                        <el-input v-model="commentForm.contentReply" placeholder="写下你的回复..." size="small"></el-input>
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

        <!-- 固定在底部的互动区域，这部分要放在blog-container的div里面 -->
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
                <span style="color: #333333cc">{{ blog.likeCount || 0 }}</span>
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
                <span style="color: #333333cc">{{ blog.collectCount || 0 }}</span>
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

</template>

<style lang="scss" scoped>

/*定义前台头部 背景 主题色*/
$front-back-color: #fff;

/*定义前台头部 字体 主题色*/
$front-font-color: #d54941;

/* 搜索关键词显示样式 */
.search-keyword-display {
  padding: 20px 0;
  margin-bottom: 20px;
  text-align: center;
}

.search-title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  .search-subtitle {
    font-size: 14px;
    color: #666;
    margin: 0;
  }

.front-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header-nav {
  z-index: 1800;
  position: sticky;
  top: 0;
  height: 70px;
  background-color: $front-back-color;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.1);
  overflow: visible;

  .header-left-warp {
    display: flex;
    align-items: center;
    height: 100%;

    .logo-warp {
      cursor: pointer;
      display: flex;
      align-items: center;
      margin-left: 20px;

      .logo {
        width: 30px;
        height: 30px;
        margin-right: 10px;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .logo-text {
        font-size: 22px;
        font-weight: 500;
        color: $front-font-color;
      }

    }

    .header-navs{
      margin-left: 80px;
      height: 100%;

      .el-menu {
        background-color: $front-back-color !important;
        border: none !important;
        height: 70px !important;
      }

      .el-menu-item {
        height: 70px !important;
        line-height: 70px !important;
        border: none !important;
      }

      .el-menu-item:hover {
        color: $front-font-color !important;
        background-color: transparent !important;
      }

      .el-menu-item.is-active {
        color: $front-font-color !important;
        background-color: transparent !important;
        border: none !important;
      }

    }

  }

  .user-warp {
    display: flex;
    align-items: center;
    margin-right: 20px;
    height: 100%; /* 确保高度与父元素一致 */

    .btn-login {
      margin-top: 0;
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      border: 1px solid $front-font-color;
      padding: 2px;
      cursor: pointer;
      outline: none !important;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
      }

    }

    .dropdown-link {
      display: flex;
      align-items: center;
      color: inherit;
      text-decoration: none;

      .el-icon {
        margin-right: 8px;
      }
    }

  }
}

.main-content {
  flex: 1;
  background-color: #fff;
  display: flex;
  gap: 30px;

  .main-left{
    width: 20%;
    padding: 20px 0;

    .sidebar-nav {
      position: sticky;
      top: 90px;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 90px);

      .sidebar-menu {
        flex: 1;
        padding: 0 20px;
        margin-left: 100px;
      }

      .sidebar-menu-item {
        display: flex;
        align-items: center;
        padding: 16px 12px;
        margin-bottom: 8px;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        color: #333;
        font-size: 16px;
        font-weight: 500;

        &:hover {
          background-color: #f8f8f8;
        }

        &.active {
          background-color: #fff2f0;
          color: $front-font-color;
          border-radius: 20px;

          .menu-icon {
            color: $front-font-color;
          }
        }

        .menu-icon {
          width: 24px;
          height: 24px;
          margin-right: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #666;
          font-size: 24px;

          .el-icon {
            font-size: 24px;
          }
        }

        .menu-text {
          flex: 1;
          font-size: 16px;
          font-weight: 500;
        }
      }
    }
  }

  .main-right{
    flex: 1;
  }

}

.front-footer {
  padding: 16px 24px;
  text-align: center;
  background-color: #fff;
  color: #666;
  font-size: 12px;
  border-top: 1px solid #eee;
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
      
      .footer-right {
        .icon {
          margin-right: 4px;
          cursor: pointer;
        }
      }

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
      
      // 轮播图样式
      .image-carousel {
        position: relative;
        width: 100%;
        height: 100%;
        
        .carousel-main {
          width: 100%;
          height: 100%;
        }
        
        .carousel-btn {
          position: absolute;
          top: 50%;
          transform: translateY(-50%);
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: rgba(0, 0, 0, 0.5);
          color: #fff;
          border: none;
          font-size: 24px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: background 0.3s;
          z-index: 10;
          
          &:hover {
            background: rgba(0, 0, 0, 0.7);
          }
          
          &-prev {
            left: 15px;
          }
          
          &-next {
            right: 15px;
          }
        }
        
        .carousel-indicators {
          position: absolute;
          bottom: 15px;
          left: 50%;
          transform: translateX(-50%);
          display: flex;
          gap: 8px;
          z-index: 10;
          
          .indicator {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.5);
            cursor: pointer;
            transition: all 0.3s;
            
            &.active {
              background: #fff;
              width: 24px;
              border-radius: 4px;
            }
            
            &:hover {
              background: rgba(255, 255, 255, 0.8);
            }
          }
        }
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
  padding-bottom: 120px;

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
        
        .icon {
          margin-right: 4px;
          cursor: pointer;
        }

      &:hover {
        color: #ff2442;
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
