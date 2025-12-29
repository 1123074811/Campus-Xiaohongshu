<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {projectName} from '../../config/config.default'
import {User, Lock, Key, DataAnalysis, QuestionFilled} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import request from "@/utils/request.js";

// 路由实例
const router = useRouter()
const userFormInst = ref(null)

// 角色选项 - 注册时只能是普通用户
const roleOptions = [
  { label: '普通用户', value: 'ROLE_USER' }
]

// 预设安全问题选项
const securityQuestions = [
  '您的出生地是哪里？',
  '您小学时最好的朋友叫什么名字？',
  '您的第一只宠物叫什么名字？',
  '您母亲的姓名是什么？',
  '您最喜欢的电影是什么？',
  '您最喜欢的书籍是什么？',
  '您的第一个老师叫什么名字？',
  '您最难忘的一次旅行是去哪里？',
  '自定义问题'
]

// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'ROLE_USER', // 默认选择普通用户
  securityQuestion: '',
  customQuestion: '',
  securityAnswer: ''
})

// 是否同意
const isAllow = ref(true)

// 表单验证规则
const rules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请确认密码', trigger: 'blur'},
    {min: 3, max: 10, message: '长度在 3 到 10 个字符', trigger: 'blur'}
  ],
  securityQuestion: [
    {required: true, message: '请选择安全问题', trigger: 'change'}
  ],
  customQuestion: [
    {
      validator: (rule, value, callback) => {
        if (registerForm.securityQuestion === '自定义问题' && !value) {
          callback(new Error('请输入自定义安全问题'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  securityAnswer: [
    {required: true, message: '请输入安全问题答案', trigger: 'blur'},
    {min: 1, max: 50, message: '答案长度在 1 到 50 个字符', trigger: 'blur'}
  ]
}

// 注册方法
const register = () => {
  // 表单校验合法
  userFormInst.value.validate((valid) => {
    // 如果合法
    if (valid) {
      // 检查两次密码是否一致
      if (registerForm.password !== registerForm.confirmPassword) {
        ElMessage.error('两次输入的新密码不相同')
        return false
      }

      // 确定最终的安全问题
      const finalSecurityQuestion = registerForm.securityQuestion === '自定义问题' 
        ? registerForm.customQuestion 
        : registerForm.securityQuestion

      // 创建注册数据对象
      const registerData = {
        username: registerForm.username,
        password: registerForm.password,
        role: registerForm.role,
        securityQuestion: finalSecurityQuestion,
        securityAnswer: registerForm.securityAnswer
      }

      request.post('/web/register', registerData).then((res) => {
        if (res.code === '200') {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        } else {
          ElMessage.error(res.msg || '注册失败')
        }
      }).catch(error => {
        console.error('注册请求错误:', error)
        ElMessage.error('注册失败，请稍后重试')
      })
    }
  })
}

// 监听安全问题选择变化
const onSecurityQuestionChange = () => {
  if (registerForm.securityQuestion !== '自定义问题') {
    registerForm.customQuestion = ''
  }
}
</script>

<template>
  <div class="register-container">
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
    </div>

    <div class="register-content">
      <div class="register-left">
        <div class="brand-logo">
          <div class="logo-circle">
            <img src="../../config/logo.svg" alt="Logo" class="logo-image" />
          </div>
          <h2 class="brand-name">{{ projectName }}</h2>
        </div>
        <div class="register-features">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="feature-text">
              <h3>用户管理</h3>
              <p>全面的用户权限管理系统</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="feature-text">
              <h3>安全可靠</h3>
              <p>多重安全防护，数据无忧</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="feature-text">
              <h3>数据融合</h3>
              <p>多方位数据存储功能融合</p>
            </div>
          </div>
        </div>
        <div class="register-footer">
          <p>© {{ new Date().getFullYear() }} {{ projectName }}. 保留所有权利</p>
        </div>
      </div>

      <div class="register-right">
        <div class="register-form-container">
          <h1 class="register-title">创建账号</h1>
          <p class="register-subtitle">请填写以下信息完成注册</p>

          <el-form :model="registerForm" :rules="rules" ref="userFormInst" class="register-form" @keydown.enter="register">
            <el-form-item prop="username">
              <el-input
                  placeholder="请输入用户名"
                  size="large"
                  :prefix-icon="User"
                  v-model="registerForm.username">
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  placeholder="请输入密码"
                  v-model="registerForm.password">
              </el-input>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <el-input
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  placeholder="请确认密码"
                  v-model="registerForm.confirmPassword">
              </el-input>
            </el-form-item>

            <!-- 安全问题设置 -->
            <div class="security-section">
              <div class="section-title">
                <el-icon><QuestionFilled /></el-icon>
                <span>设置安全问题（用于找回密码）</span>
              </div>
              
              <el-form-item prop="securityQuestion">
                <el-select
                    v-model="registerForm.securityQuestion"
                    placeholder="请选择安全问题"
                    size="large"
                    style="width: 100%"
                    @change="onSecurityQuestionChange">
                  <el-option
                      v-for="question in securityQuestions"
                      :key="question"
                      :label="question"
                      :value="question">
                  </el-option>
                </el-select>
              </el-form-item>

              <el-form-item v-if="registerForm.securityQuestion === '自定义问题'" prop="customQuestion">
                <el-input
                    v-model="registerForm.customQuestion"
                    placeholder="请输入您的自定义安全问题"
                    size="large"
                    maxlength="100"
                    show-word-limit>
                </el-input>
              </el-form-item>

              <el-form-item prop="securityAnswer">
                <el-input
                    v-model="registerForm.securityAnswer"
                    placeholder="请输入安全问题的答案"
                    size="large"
                    maxlength="50"
                    show-word-limit>
                </el-input>
              </el-form-item>
            </div>


            <el-form-item>
              <el-checkbox v-model="isAllow" class="allow-warp">
                我已阅读并同意 <a href="#" class="link">《隐私政策》</a>和<a href="#" class="link">《服务条款》</a>
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button
                  class="register-button"
                  type="primary"
                  size="large"
                  :disabled="!isAllow"
                  @click="register">
                注册
              </el-button>
            </el-form-item>

            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div class="back-to-home">
                <el-button
                    type="text"
                    size="large"
                    @click="router.push('/front/home')"
                    class="back-button"
                >
                  ← 返回首页
                </el-button>
              </div>

              <div class="login-link">
                已有账号？<a @click="router.push('/login')" class="link">立即登录</a>
              </div>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.register-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f0f4f8 0%, #d7e3f3 100%);
  position: relative;
  overflow: hidden;
}

.background-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 0;

  .shape {
    position: absolute;
    border-radius: 50%;

    &-1 {
      width: 300px;
      height: 300px;
      background: linear-gradient(45deg, rgba(64, 132, 217, 0.1) 0%, rgba(64, 132, 217, 0.05) 100%);
      top: -100px;
      left: -100px;
    }

    &-2 {
      width: 200px;
      height: 200px;
      background: linear-gradient(45deg, rgba(126, 87, 194, 0.1) 0%, rgba(126, 87, 194, 0.05) 100%);
      bottom: -50px;
      right: 10%;
    }

    &-3 {
      width: 150px;
      height: 150px;
      background: linear-gradient(45deg, rgba(66, 165, 245, 0.1) 0%, rgba(66, 165, 245, 0.05) 100%);
      top: 20%;
      right: -50px;
    }

    &-4 {
      width: 250px;
      height: 250px;
      background: linear-gradient(45deg, rgba(239, 83, 80, 0.08) 0%, rgba(239, 83, 80, 0.04) 100%);
      bottom: 10%;
      left: 10%;
    }
  }
}

.register-content {
  width: 1000px;
  height: 750px; // 增加高度以适应更多表单项
  display: flex;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);

  @media (max-width: 1024px) {
    width: 90%;
    height: auto;
    min-height: 700px;
  }

  @media (max-width: 768px) {
    flex-direction: column;
    width: 95%;
    max-width: 500px;
  }
}

.register-left {
  width: 40%;
  background: linear-gradient(135deg, #4084d9 0%, #1a4f94 100%);
  color: white;
  padding: 40px;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;

  @media (max-width: 768px) {
    width: 100%;
    padding: 30px 20px;
    min-height: auto;
  }
}

.brand-logo {
  display: flex;
  align-items: center;
  margin-bottom: 60px;
  position: relative;
  z-index: 1;

  @media (max-width: 768px) {
    margin-bottom: 30px;
    justify-content: center;
  }

  .logo-circle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background-color: white;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 12px;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);

    .logo-image {
      width: 40px;
      height: 40px;
      object-fit: contain;
    }
  }

  .brand-name {
    font-size: 20px;
    font-weight: 600;

    @media (max-width: 480px) {
      font-size: 18px;
    }
  }
}

.register-features {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;

  @media (max-width: 768px) {
    display: none;
  }

  .feature-item {
    display: flex;
    align-items: center;
    margin-bottom: 30px;

    .feature-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background-color: rgba(255, 255, 255, 0.2);
      display: flex;
      justify-content: center;
      align-items: center;
      margin-right: 16px;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);

      .el-icon {
        font-size: 24px;
      }
    }

    .feature-text {
      h3 {
        font-size: 16px;
        margin: 0 0 5px 0;
        font-weight: 600;
      }

      p {
        font-size: 14px;
        margin: 0;
        opacity: 0.8;
      }
    }
  }
}

.register-footer {
  font-size: 12px;
  opacity: 0.7;
  text-align: center;
  position: relative;
  z-index: 1;

  @media (max-width: 768px) {
    display: none;
  }
}

.register-right {
  width: 60%;
  background-color: transparent;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;

  @media (max-width: 768px) {
    width: 100%;
    padding: 30px 20px;
  }
}

.register-form-container {
  width: 360px;
  position: relative;
  z-index: 1;
  max-height: 650px; // 增加最大高度
  overflow-y: auto;
  padding-right: 10px;

  @media (max-width: 480px) {
    width: 100%;
    max-height: none;
  }
}

// 安全问题区域样式
.security-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin: 20px 0;
  border: 1px solid #e9ecef;

  .section-title {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
    font-size: 14px;
    font-weight: 500;
    color: #495057;

    .el-icon {
      margin-right: 8px;
      color: #4084d9;
    }
  }

  .el-form-item {
    margin-bottom: 15px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;

  @media (max-width: 480px) {
    font-size: 24px;
  }
}

.register-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0 0 30px 0;

  @media (max-width: 480px) {
    font-size: 14px;
    margin: 0 0 20px 0;
  }
}

.back-to-home {
  display: flex;
  justify-content: flex-start;
  
  .back-button {
    font-size: 14px;
    color: #4084d9;
    padding: 0;
    margin: 0;
    height: auto;
    line-height: normal;
    
    &:hover {
      color: #3a76c4;
      background-color: transparent;
    }
  }
}

.register-form {
  .allow-warp {
    font-size: 14px;
    color: #666;
  }

  .register-button {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    margin-top: 10px;
    background: linear-gradient(to right, #4084d9, #1a4f94);
    border: none;

    &:hover {
      background: linear-gradient(to right, #3a76c4, #164785);
    }

    &:disabled {
      background: linear-gradient(to right, #a0c3f0, #8eadd3);
    }
  }
}

.login-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.link {
  color: #4084d9;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}
</style>
