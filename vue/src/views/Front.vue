<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { projectName } from '../../config/config.default'
import { User, Lock, SwitchButton, VideoCamera, Bell, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 路由实例
const router = useRouter()
const route = useRoute()

// 用户信息
const account = ref(
    localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {}
)

// 当前激活的菜单项
const activeMenu = computed(() => route.path)

// 退出登录
const logout = () => {
  localStorage.removeItem('account')
  ElMessage.success('退出成功')
  router.push('/login')
}

const handleUpdateAccount = (updatedAccount) => {
  // 更新父组件中的用户信息
  account.value = updatedAccount
}

const activeLeftMenu = ref('/front/home')

const menus = ref([
    {
      path:'/front/home',
      name:'发现',
      icon: 'House',
    },
    {
      path:'/front/publish',
      name:'发布',
      icon: 'VideoCamera',
    },
    {
      path:'/front/message',
      name:'通知',
      icon: 'Bell',
    },
    {
      path:'/front/user',
      name:'我',
      icon: 'avatar',
    },
])

const changeActivityMenu = (menu) =>{
  activeLeftMenu.value = menu.path

  router.push(menu.path)
}

const keyword = ref('')

const search = () => {
  router.push({
    path: '/front/search',
    query: {
      keyword: keyword.value
    }
  })
}

</script>

<template>

<!--  回到顶部-->
  <el-backtop :right="50" :bottom="50" />

  <div class="front-container">
    <!-- 顶部导航栏 -->
    <header class="header-nav">
      <div class="header-left-warp">
        <div class="logo-warp">
          <div class="logo">
            <img src="../../config/logo.svg" alt="Logo" />
          </div>
          <div class="logo-text">{{ projectName }}</div>
        </div>

        <div class="header-navs">
          <el-menu
              router
              :default-active="activeMenu"
              mode="horizontal"
              :ellipsis="false"
          >
            <!--前台路由-->
<!--            <el-menu-item index="/front/home">前台首页</el-menu-item>-->
            <!--前台路由-->
          </el-menu>
        </div>
      </div>

      <div style="display: flex">
        <el-input v-model="keyword" size="large" placeholder="请输入小红书" prefix-icon="Search" clearable style="width: 406px"/>
        <el-button type="danger" size="large" style="margin-left: 5px" @click="search">搜索</el-button>
      </div>

      <div class="user-warp">
        <!-- 未登录状态显示登录注册按钮 -->
        <template v-if="!account.id">
          <div class="btn-login">
            <el-button @click="router.push('/login')">登录</el-button>
          </div>
          <div class="btn-login" style="margin-left: 10px">
            <el-button @click="router.push('/register')">注册</el-button>
          </div>
        </template>

        <!-- 已登录状态显示用户头像和下拉菜单 -->
        <el-dropdown v-else class="custom-dropdown">
          <div class="user-avatar">
            <img :src="account.avatarUrl" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>{{ account.nickname }}</el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/person" class="dropdown-link">
                  <el-icon><User /></el-icon>
                  <span>个人信息</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <router-link to="/front/password" class="dropdown-link">
                  <el-icon><Lock /></el-icon>
                  <span>修改密码</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <div @click="logout" class="dropdown-link">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区域 -->
    <div class="main-content">


      <div style="width: 20%">

      <!---左侧菜单部分-->
      <div style="margin-left: 150px;margin-top: 20px">
        <div class="menu-item" v-for="menu in menus" :key="menu.path" @click="changeActivityMenu(menu)" :class="{activeMenu: menu.path===activeMenu}">
          <div style="font-size: 20px;display: flex; align-items: center">
            <el-icon v-if="menu.icon==='House'"><House /></el-icon>
            <el-icon v-if="menu.icon==='VideoCamera'"><VideoCamera /></el-icon>
            <el-icon v-if="menu.icon==='Bell'"><Bell /></el-icon>
            <div v-if="menu.icon==='avatar'">
              <el-avatar :src="account.avatarUrl" :size="20"></el-avatar>
            </div>
          </div>
          <div style="display: flex; align-items: center">
            {{menu.name}}
          </div>
        </div>
      </div>
      </div>
      <div style="flex: 1; padding: 20px; position: relative;">
        <!---右侧内容部分-->
        <router-view @update-account="handleUpdateAccount"></router-view>
      </div>
    </div>

    <!-- 页脚 -->
    <footer class="front-footer">
      <p>© {{ new Date().getFullYear() }} {{ projectName }}. 保留所有权利</p>
    </footer>
  </div>
</template>

<style lang="scss" scoped>

/*定义前台头部 背景 主题色*/
$front-back-color: #fff;

/*定义前台头部 字体 主题色*/
$front-font-color: #d54941;

.menu-item {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  height: 40px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 20px;
}

.menu-item:hover {
  background-color: #f5f5f5;
  border-radius: 20px;
}

/* 提高activeMenu的优先级，确保选中状态覆盖悬浮状态 */
.menu-item.activeMenu,
.menu-item.activeMenu:hover {
  background-color: #fff2f0;
  border-radius: 20px;
  color: #d54941;
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
  gap: 20px;
}

.front-footer {
  padding: 16px 24px;
  text-align: center;
  background-color: #fff;
  color: #666;
  font-size: 12px;
  border-top: 1px solid #eee;
}

</style>
