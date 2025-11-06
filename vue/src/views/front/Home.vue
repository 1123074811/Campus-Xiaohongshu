<script setup>
import {ref, onMounted, reactive, computed, nextTick} from "vue";
import request from "@/utils/request.js";
import { Star, StarFilled } from '@element-plus/icons-vue';

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
  getWaterfallContainerWidth()
  loadUsers()
  load(false)
})

const searchForm = reactive({
  typeId: 0,
  keyword: ''
})

const changeTypeId = (id) => {
  searchForm.typeId = id
  load(false)
}

//表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

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
  // 防止容器宽度为0时出现负数
  return Math.max(0, Math.floor((waterfallContainerWidth.value - totalBlogWidth) / col.value))
})

const waterfallRef = ref(null)

const blog = ref({})
const blogVisible = ref(false)
const dialogStyle = ref({})
const originalPosition = ref({})
const isClosing = ref(false)

const showBlog = (item, event) => {

  blog.value = item

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

</script>

<template>

  <div class="type-container">
    <div v-for="type in types" :key="type.id" class="type-item"
         :style="{ fontWeight: (searchForm.typeId === type.id ? 'bold' : 'normal') }" @click="changeTypeId(type.id)">
      {{ type.name }}
    </div>
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
            <div class="footer-right">
              <el-icon v-if="blog.isCollected"><StarFilled /></el-icon>
              <el-icon v-else><Star /></el-icon>
              <span>{{ blog.count || 0 }}</span>
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
        <el-image :src="blog.img" class="img" :preview-src-list="[blog.img]" v-else></el-image>
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


        </div>

      </div>
    </div>
  </div>

</template>

<style scoped>
.type-container {
  display: flex;
  flex-wrap: wrap;
  margin: 20px 0;
}

.type-item {
  padding: 10px 20px;
  margin: 5px;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s, color 0.3s;
  background-color: #f1f1f1;
  color: #333;
  font-size: 16px;
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.1);
}

.type-item.active {
  background-color: #f6a600;
  color: #fff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
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


</style>