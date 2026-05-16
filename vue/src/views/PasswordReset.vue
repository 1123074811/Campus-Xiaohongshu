<template>
  <div class="password-reset-container">
    <div class="reset-card">
      <div class="card-header">
        <h2>找回密码</h2>
        <p>通过安全问题重置您的密码</p>
      </div>

      <!-- 步骤指示器 -->
      <div class="steps">
        <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
          <div class="step-number">1</div>
          <div class="step-title">输入用户名</div>
        </div>
        <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
          <div class="step-number">2</div>
          <div class="step-title">回答安全问题</div>
        </div>
        <div class="step" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
          <div class="step-number">3</div>
          <div class="step-title">设置新密码</div>
        </div>
      </div>

      <!-- 步骤1: 输入用户名 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-form :model="usernameForm" :rules="usernameRules" ref="usernameFormRef">
          <el-form-item label="用户类型" prop="userType">
            <el-radio-group v-model="usernameForm.userType">
              <el-radio label="USER">普通用户</el-radio>
              <el-radio label="ADMIN">管理员</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="usernameForm.username"
              placeholder="请输入用户名"
              size="large"
              @keyup.enter="getSecurityQuestion"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              style="width: 100%"
              :loading="loading"
              @click="getSecurityQuestion"
            >
              下一步
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2: 回答安全问题 -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="security-question">
          <h3>安全问题</h3>
          <p class="question-text">{{ securityQuestion.securityQuestion }}</p>
        </div>
        
        <el-form :model="answerForm" :rules="answerRules" ref="answerFormRef">
          <el-form-item label="答案" prop="answer">
            <el-input
              v-model="answerForm.answer"
              placeholder="请输入安全问题的答案"
              size="large"
              @keyup.enter="verifyAnswer"
            />
          </el-form-item>
          
          <el-form-item>
            <div class="button-group">
              <el-button size="large" @click="goBack">上一步</el-button>
              <el-button 
                type="primary" 
                size="large"
                :loading="loading"
                @click="verifyAnswer"
              >
                验证答案
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤3: 设置新密码 -->
      <div v-if="currentStep === 3" class="step-content">
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef">
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              size="large"
              show-password
            />
          </el-form-item>
          
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              size="large"
              show-password
              @keyup.enter="resetPassword"
            />
          </el-form-item>
          
          <el-form-item>
            <div class="button-group">
              <el-button size="large" @click="goBack">上一步</el-button>
              <el-button 
                type="primary" 
                size="large"
                :loading="loading"
                @click="resetPassword"
              >
                重置密码
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 成功页面 -->
      <div v-if="currentStep === 4" class="step-content success-content">
        <div class="success-icon">
          <el-icon size="60" color="#67C23A"><SuccessFilled /></el-icon>
        </div>
        <h3>密码重置成功！</h3>
        <p>您的密码已成功重置，请使用新密码登录。</p>
        <el-button type="primary" size="large" @click="goToLogin">
          返回登录
        </el-button>
      </div>

      <!-- 返回登录链接 -->
      <div v-if="currentStep < 4" class="footer-links">
        <router-link to="/login" class="link">返回登录</router-link>
        <span class="separator">|</span>
        <router-link to="/register" class="link">注册账号</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SuccessFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

// 当前步骤
const currentStep = ref(1)
const loading = ref(false)

// 用户名表单
const usernameForm = reactive({
  username: '',
  userType: 'USER'
})

const usernameRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

// 安全问题信息
const securityQuestion = ref({})

// 答案表单
const answerForm = reactive({
  answer: ''
})

const answerRules = {
  answer: [
    { required: true, message: '请输入安全问题的答案', trigger: 'blur' }
  ]
}

// 密码表单
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 表单引用
const usernameFormRef = ref()
const answerFormRef = ref()
const passwordFormRef = ref()

// 获取安全问题
const getSecurityQuestion = async () => {
  if (!usernameFormRef.value) return
  
  const valid = await usernameFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    const res = await request.get('/password-reset/security-question', {
      params: {
        username: usernameForm.username,
        userType: usernameForm.userType
      }
    })
    
    if (res.code === '200') {
      securityQuestion.value = res.data
      currentStep.value = 2
      ElMessage.success('找到安全问题')
    } else {
      ElMessage.error(res.msg || '获取安全问题失败')
    }
  } catch (error) {
    console.error('获取安全问题失败:', error)
    ElMessage.error('获取安全问题失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 验证安全问题答案
const verifyAnswer = async () => {
  if (!answerFormRef.value) return
  
  const valid = await answerFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    const res = await request.post('/password-reset/verify-answer', null, {
      params: {
        username: usernameForm.username,
        userType: usernameForm.userType,
        answer: answerForm.answer
      }
    })
    
    if (res.code === '200') {
      currentStep.value = 3
      ElMessage.success('验证通过')
    } else {
      ElMessage.error(res.msg || '安全问题答案错误')
    }
  } catch (error) {
    console.error('验证答案失败:', error)
    ElMessage.error('验证失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 重置密码
const resetPassword = async () => {
  if (!passwordFormRef.value) return
  
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  
  try {
    const res = await request.post('/password-reset/reset', {
      username: usernameForm.username,
      userType: usernameForm.userType,
      securityAnswer: answerForm.answer,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    
    if (res.code === '200') {
      currentStep.value = 4
      ElMessage.success('密码重置成功')
    } else {
      ElMessage.error(res.msg || '密码重置失败')
    }
  } catch (error) {
    console.error('重置密码失败:', error)
    ElMessage.error('重置密码失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 返回上一步
const goBack = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.password-reset-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.reset-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 500px;
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
}

.card-header h2 {
  color: #333;
  margin-bottom: 8px;
  font-size: 28px;
  font-weight: 600;
}

.card-header p {
  color: #666;
  font-size: 14px;
}

/* 步骤指示器 */
.steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40px;
  position: relative;
}

.steps::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #e4e7ed;
  z-index: 1;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e4e7ed;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.step.active .step-number {
  background: #409eff;
  color: white;
}

.step.completed .step-number {
  background: #67c23a;
  color: white;
}

.step-title {
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.step.active .step-title,
.step.completed .step-title {
  color: #333;
  font-weight: 500;
}

/* 步骤内容 */
.step-content {
  margin-bottom: 20px;
}

/* 设置表单标签宽度，使输入框对齐 */
.step-content :deep(.el-form-item__label) {
  width: 80px;
  text-align: right;
}

.security-question {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  border-left: 4px solid #409eff;
}

.security-question h3 {
  color: #333;
  margin-bottom: 10px;
  font-size: 16px;
}

.question-text {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.button-group {
  display: flex;
  gap: 12px;
}

.button-group .el-button {
  flex: 1;
}

/* 成功页面 */
.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  margin-bottom: 20px;
}

.success-content h3 {
  color: #333;
  margin-bottom: 10px;
  font-size: 20px;
}

.success-content p {
  color: #666;
  margin-bottom: 30px;
  line-height: 1.5;
}

/* 底部链接 */
.footer-links {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.link {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.link:hover {
  color: #66b1ff;
}

.separator {
  margin: 0 12px;
  color: #dcdfe6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .reset-card {
    padding: 30px 20px;
    margin: 10px;
  }
  
  .card-header h2 {
    font-size: 24px;
  }
  
  .steps {
    margin-bottom: 30px;
  }
  
  .step-number {
    width: 35px;
    height: 35px;
  }
  
  .step-title {
    font-size: 11px;
  }
}
</style>