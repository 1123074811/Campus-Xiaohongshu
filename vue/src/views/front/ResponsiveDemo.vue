<template>
  <div class="responsive-demo">
    <div class="responsive-container">
      <h1 class="text-xxl text-center">响应式设计演示</h1>
      
      <!-- 网格布局演示 -->
      <section class="demo-section">
        <h2 class="text-xl m-md">网格布局</h2>
        <div class="grid grid-cols-4 gap-md">
          <div class="demo-card" v-for="i in 8" :key="i">
            <div class="card-content">
              <h3 class="text-lg">卡片 {{ i }}</h3>
              <p class="text-sm">这是一个响应式卡片，会根据屏幕大小自动调整布局。</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Flex布局演示 -->
      <section class="demo-section">
        <h2 class="text-xl m-md">Flex布局</h2>
        <div class="flex flex-wrap justify-between items-center gap-md">
          <div class="flex-item">
            <el-button type="primary">主要按钮</el-button>
          </div>
          <div class="flex-item">
            <el-button type="success">成功按钮</el-button>
          </div>
          <div class="flex-item">
            <el-button type="warning">警告按钮</el-button>
          </div>
          <div class="flex-item">
            <el-button type="danger">危险按钮</el-button>
          </div>
        </div>
      </section>

      <!-- 响应式显示/隐藏演示 */
      <section class="demo-section">
        <h2 class="text-xl m-md">响应式显示控制</h2>
        <div class="visibility-demo">
          <div class="show-desktop p-md rounded-lg shadow-md">
            <h3 class="text-lg">桌面端内容</h3>
            <p class="text-sm">这个内容只在桌面端显示（屏幕宽度 > 768px）</p>
          </div>
          <div class="show-mobile p-md rounded-lg shadow-md">
            <h3 class="text-lg">移动端内容</h3>
            <p class="text-sm">这个内容只在移动端显示（屏幕宽度 ≤ 768px）</p>
          </div>
        </div>
      </section>

      <!-- 表单演示 */
      <section class="demo-section">
        <h2 class="text-xl m-md">响应式表单</h2>
        <div class="form-demo">
          <el-form :model="form" label-width="120px" class="responsive-form">
            <div class="grid grid-cols-2 gap-md">
              <el-form-item label="用户名">
                <el-input v-model="form.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
            </div>
            <el-form-item label="个人简介">
              <el-input 
                v-model="form.bio" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入个人简介" 
              />
            </el-form-item>
            <el-form-item>
              <div class="flex gap-md">
                <el-button type="primary">提交</el-button>
                <el-button>重置</el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </section>

      <!-- 图片画廊演示 -->
      <section class="demo-section">
        <h2 class="text-xl m-md">响应式图片画廊</h2>
        <div class="gallery-demo">
          <div class="grid grid-cols-3 gap-sm">
            <div 
              v-for="i in 9" 
              :key="i" 
              class="gallery-item rounded-md shadow-sm"
            >
              <img 
                :src="`https://picsum.photos/300/200?random=${i}`" 
                :alt="`图片 ${i}`"
                class="gallery-image"
              />
            </div>
          </div>
        </div>
      </section>

      <!-- 设备信息显示 -->
      <section class="demo-section">
        <h2 class="text-xl m-md">当前设备信息</h2>
        <div class="device-info p-md rounded-lg shadow-md">
          <p class="text-sm"><strong>屏幕宽度:</strong> {{ screenWidth }}px</p>
          <p class="text-sm"><strong>屏幕高度:</strong> {{ screenHeight }}px</p>
          <p class="text-sm"><strong>设备类型:</strong> {{ deviceType }}</p>
          <p class="text-sm"><strong>方向:</strong> {{ orientation }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const form = ref({
  username: '',
  email: '',
  bio: ''
})

const screenWidth = ref(0)
const screenHeight = ref(0)
const deviceType = ref('')
const orientation = ref('')

const updateScreenInfo = () => {
  screenWidth.value = window.innerWidth
  screenHeight.value = window.innerHeight
  
  if (screenWidth.value < 480) {
    deviceType.value = '小型手机'
  } else if (screenWidth.value < 768) {
    deviceType.value = '大型手机'
  } else if (screenWidth.value < 1024) {
    deviceType.value = '平板'
  } else if (screenWidth.value < 1280) {
    deviceType.value = '小型桌面'
  } else {
    deviceType.value = '大型桌面'
  }
  
  orientation.value = screenWidth.value > screenHeight.value ? '横屏' : '竖屏'
}

onMounted(() => {
  updateScreenInfo()
  window.addEventListener('resize', updateScreenInfo)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateScreenInfo)
})
</script>

<style scoped>
.responsive-demo {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: var(--spacing-lg) 0;
}

.demo-section {
  margin-bottom: var(--spacing-xl);
  background-color: white;
  border-radius: 12px;
  padding: var(--spacing-lg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.demo-card {
  background-color: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s ease;
}

.demo-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-content {
  padding: var(--spacing-md);
}

.card-content h3 {
  margin: 0 0 var(--spacing-sm) 0;
  color: #333;
}

.card-content p {
  margin: 0;
  color: #666;
  line-height: 1.5;
}

.flex-item {
  flex: 1;
  min-width: 120px;
}

.visibility-demo {
  display: flex;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.visibility-demo > div {
  flex: 1;
  min-width: 250px;
  background-color: #e3f2fd;
  border: 1px solid #90caf9;
}

.show-mobile {
  background-color: #f3e5f5 !important;
  border-color: #ce93d8 !important;
}

.form-demo {
  background-color: #fafafa;
  padding: var(--spacing-lg);
  border-radius: 8px;
}

.responsive-form {
  max-width: 600px;
}

.gallery-demo {
  background-color: #fafafa;
  padding: var(--spacing-md);
  border-radius: 8px;
}

.gallery-item {
  overflow: hidden;
  background-color: white;
  transition: transform 0.2s ease;
}

.gallery-item:hover {
  transform: scale(1.02);
}

.gallery-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.device-info {
  background-color: #e8f5e8;
  border: 1px solid #4caf50;
}

.device-info p {
  margin: var(--spacing-xs) 0;
}

.text-center {
  text-align: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .demo-section {
    padding: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
  }
  
  .visibility-demo {
    flex-direction: column;
  }
  
  .flex-item {
    min-width: 100%;
  }
  
  .gallery-image {
    height: 120px;
  }
}

@media (max-width: 480px) {
  .responsive-demo {
    padding: var(--spacing-md) 0;
  }
  
  .demo-section {
    padding: var(--spacing-sm);
    margin-bottom: var(--spacing-md);
  }
  
  .gallery-image {
    height: 100px;
  }
}
</style>