import { createRouter, createWebHistory } from 'vue-router'
import { projectName } from '../../config/config.default'

const routes = [
  //通用路由
  {
    path: '/',
    name: '/',
    component: () => import('../views/Login.vue'),
    meta: {
      title: '登录'
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: {
      title: '登录'
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: {
      title: '注册'
    }
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404.vue'),
    meta: {
      title: '404'
    }
  },
  //下面都是前台路由
  {
    path: '/front',
    name: 'Front',
    component: () => import('../views/Front.vue'),
    children: [
      // 前台子路由
      {
        path: 'home',
        name: 'FrontHome',
        component: () => import('../views/front/Home.vue'),
        meta: {
          title: '发现精彩'
        }
      },
      {
        path: 'password',
        name: 'FrontPassword',
        component: () => import('../views/front/Password.vue'),
        meta: {
          title: '修改密码'
        }
      },
      {
        path: 'person',
        name: 'FrontPerson',
        component: () => import('../views/front/Person.vue'),
        meta: {
          title: '个人信息'
        }
      },
      {
        path: 'publish',
        name: 'FrontPublish',
        component: () => import('../views/front/Publish.vue'),
        meta: {
          title: '发布页面'
        }
      },
      {
        path: 'message',
        name: 'FrontMessage',
        component: () => import('../views/front/Message.vue'),
        meta: {
          title: '消息通知'
        }
      },
      {
        path: 'chat',
        name: 'FrontChat',
        component: () => import('../views/front/Chat.vue'),
        meta: {
          title: '聊天'
        }
      },
      {
        path: 'user',
        name: 'FrontUser',
        component: () => import('../views/front/User.vue'),
        meta: {
          title: '个人详情'
        }
      },
      {
        path: 'search',
        name: 'FrontSearch',
        component: () => import('../views/front/Search.vue'),
        meta: {
          title: '搜索页面'
        }
      },
      // 前台子路由
    ]
  },
  //下面都是后台路由
  {
    path: '/back',
    name: 'back',
    component: () => import('../views/Back.vue'),
    children: [
      // 后台子路由
      {
        path: 'home',
        name: 'BackHome',
        component: () => import('../views/back/Home.vue'),
        meta: {
          title: '后台首页'
        }
      },
      {
        path: 'password',
        name: 'BackPassword',
        component: () => import('../views/back/Password.vue'),
        meta: {
          title: '修改密码'
        }
      },
      {
        path: 'adminPerson',
        name: 'BackAdminPerson',
        component: () => import('../views/back/AdminPerson.vue'),
        meta: {
          title: '个人信息'
        }
      },
      {
        path: 'user',
        name: 'BackUser',
        component: () => import('../views/back/User.vue'),
        meta: {
          title: '用户管理'
        }
      },
      {
        path: 'type',
        name: 'BackType',
        component: () => import('../views/back/Type.vue'),
        meta: {
          title: '分类管理'
        }
      },
      {
        path: 'blog',
        name: 'BackBlog',
        component: () => import('../views/back/Blog.vue'),
        meta: {
          title: '博客管理'
        }
      },
      {
        path: 'comment',
        name: 'BackComment',
        component: () => import('../views/back/Comment.vue'),
        meta: {
          title: '评论管理'
        }
      },
      {
        path: 'collect',
        name: 'BackCollect',
        component: () => import('../views/back/Collect.vue'),
        meta: {
          title: '收藏管理'
        }
      },
      {
        path: 'like',
        name: 'BackLike',
        component: () => import('../views/back/Like.vue'),
        meta: {
          title: '点赞管理'
        }
      },
      {
        path: 'follow',
        name: 'BackFollow',
        component: () => import('../views/back/Follow.vue'),
        meta: {
          title: '关注管理'
        }
      },
      {
        path: 'message',
        name: 'BackMessage',
        component: () => import('../views/back/Message.vue'),
        meta: {
          title: '消息管理'
        }
      },
      {
        path: 'word',
        name: 'BackWord',
        component: () => import('../views/back/Word.vue'),
        meta: {
          title: '敏感词管理'
        }
      },
      {
        path: 'admin',
        name: 'BackAdmin',
        component: () => import('../views/back/Admin.vue'),
        meta: {
          title: '管理员管理'
        }
      },
      {
        path: 'chat',
        name: 'BackChat',
        component: () => import('../views/back/Chat.vue'),
        meta: {
          title: '管理员管理'
        }
      },
      // 后台子路由
    ]
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const account = JSON.parse(localStorage.getItem("account") || '{}')
  if (to.matched.length === 0) {
    next('/404')
    return
  }
  if (to.path === '/') {
    if (account.role) {
      // 已登录用户根据角色跳转到对应首页
      if (account.role === 'ROLE_ADMIN') {
        next('/back/home')
      } else {
        next('/front/home')
      }
    } else {
      // 未登录用户直接访问前台首页
      next('/front/home')
    }
  } else {
    next()
  }
})

// 全局后置守卫
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - ${projectName}` : projectName // 设置页面标题
})

export default router
