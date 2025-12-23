<script setup>
import {ref, reactive, onMounted, nextTick, shallowRef} from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

import axios from 'axios'
import request from '../../utils/request'
import { serverHost } from '../../../config/config.default'

// 响应式数据
const form = reactive({
  name: '',
  typeId: '',
  content: '',
  img: '',
  images: '',
  video: '',
  category: ''
})

// 内容富文本
const htmlContent = ref('');
const editorRefContent = shallowRef();

// 多图上传相关
const imageList = ref([]);

const ruleFormRef = ref(null)
const videoRef = ref(null)
const canvasRef = ref(null)

// 新增：视频封面预览和提交状态
const videoCoverPreview = ref('') // 用于预览的封面图片（base64）
const isSubmitting = ref(false) // 提交状态

// 表单验证规则
const rules = reactive({
  name: [
    { required: true, message: '请输入标题', trigger: 'blur' }
  ],
  typeId: [
    { required: true, message: '请选择分类', trigger: 'blur' }
  ]
})

// 自定义上传方法
const customUpload = (file, insertFn) => {
  const formData = new FormData()
  formData.append('file', file)
  axios({
    url: `${serverHost}/web/upload`,
    method: 'post',
    data: formData,
    headers: {'Content-Type': 'multipart/form-data'},
  }).then(res => {
    insertFn(res.data)
  })
}

// wangEditor 配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
    uploadVideo: {
      customUpload: (file, insertFn) => {
        customUpload(file, insertFn)
      },
    },
  }
}

// 切换分类标签
const changeCategory = (tab) => {
  console.log('切换标签页:', tab.index)
  if (tab.index === 0) {
    // 上传图片标签页
    form.category = '图片'
    form.video = ''
    videoCoverPreview.value = ''
  } else if (tab.index === 1) {
    // 上传视频标签页
    form.category = '视频'
    form.img = ''
    imageList.value = []
  }
  console.log('当前类型:', form.category)
}

// 重置表单
const resetForm = () => {
  Object.keys(form).forEach(key => {
    form[key] = ''
  })
  htmlContent.value = ''
  imageList.value = []
  videoCoverPreview.value = ''
}

// 提交表单
const submitForm = () => {
  console.log('开始提交表单...')

  // 根据上传内容自动设置 category
  if (form.video) {
    form.category = '视频'
    console.log('检测到视频，设置 category 为: 视频')
  } else if (imageList.value.length > 0) {
    form.category = '多图'
    form.images = JSON.stringify(imageList.value)
    console.log('检测到多图，设置 category 为: 多图')
    // 如果没有设置封面，使用第一张图作为封面
    if (!form.img && imageList.value.length > 0) {
      form.img = imageList.value[0]
    }
  } else if (form.img) {
    form.category = '图片'
    console.log('检测到封面图，设置 category 为: 图片')
  }

  console.log('提交数据:', {
    name: form.name,
    typeId: form.typeId,
    category: form.category,
    img: form.img,
    video: form.video,
    images: form.images,
    contentLength: form.content?.length || 0
  })

  request.post('/blog', form).then(res => {
    console.log('提交响应:', res)
    if (res.code === '200') {
      ElMessage.success('投稿成功')
      resetForm()
    } else {
      ElMessage.error(res.msg || '投稿失败')
    }
    isSubmitting.value = false
  }).catch(error => {
    console.error('提交失败:', error)
    ElMessage.error('提交失败，请重试')
    isSubmitting.value = false
  })
}

// 生成视频封面（仅用于预览）
const generateVideoCoverPreview = () => {
  const video = videoRef.value
  const canvas = canvasRef.value

  if (!video || !canvas) return

  const ctx = canvas.getContext('2d')
  video.crossOrigin = 'anonymous'

  video.onloadeddata = () => {
    video.currentTime = 0
  }

  video.onseeked = () => {
    canvas.width = video.videoWidth || video.clientWidth
    canvas.height = video.videoHeight || video.clientHeight
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    videoCoverPreview.value = canvas.toDataURL('image/png')
  }
}

const uploadVideoCoverAndSubmit = () => {
  console.log('开始上传视频封面图...')

  if (!videoCoverPreview.value) {
    console.warn('没有视频封面预览图，直接提交')
    submitForm()
    return
  }

  // 将预览图转换为 Blob
  const imgSrcBase64 = videoCoverPreview.value
  const byteString = window.atob(imgSrcBase64.split(',')[1])
  const mimeString = imgSrcBase64.split(',')[0].split(':')[1].split(';')[0]
  const ab = new ArrayBuffer(byteString.length)
  const ia = new Uint8Array(ab)

  for (let i = 0; i < byteString.length; i++) {
    ia[i] = byteString.charCodeAt(i)
  }

  const blob = new Blob([ab], {type: mimeString})

  // 组装文件上传对象
  const formData = new FormData()
  formData.append('file', blob, 'cover_' + Date.now() + '.png')  // 增加时间戳确保唯一

  // 通过axios发送网络请求到后端上传接口
  axios({
    url: `${serverHost}/web/upload`,
    method: 'post',
    data: formData,
    headers: {'Content-Type': 'multipart/form-data'}
  }).then(res => {
    console.log('视频封面图上传成功 - 原始响应:', res)

    // 检查响应格式
    let coverUrl = ''
    if (typeof res.data === 'string') {
      coverUrl = res.data
    } else if (res.data && res.data.data) {
      coverUrl = res.data.data
    } else {
      console.error('未知的响应格式:', res)
      ElMessage.error('封面图上传响应格式错误')
      isSubmitting.value = false
      return
    }

    form.img = coverUrl
    console.log('视频封面图URL:', form.img)
    submitForm()
  }).catch(error => {
    console.error('封面图上传失败:', error)
    ElMessage.error('封面图上传失败')
    isSubmitting.value = false
  })
}

// 保存表单
const save = () => {
  ruleFormRef.value.validate((valid) => {
    if (!valid) return

    isSubmitting.value = true
    form.content = htmlContent.value

    if (form.category === '视频' && form.video && !form.img) {
      uploadVideoCoverAndSubmit()
    } else {
      submitForm()
    }
  })
}

// 图片上传成功回调
const handleImgUploadSuccess = (res) => {
  console.log('封面图上传成功 - 原始响应:', res)
  // 检查响应格式
  if (typeof res === 'string') {
    form.img = res
  } else if (res && res.data) {
    form.img = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('图片上传响应格式错误')
    return
  }
  console.log('封面图URL:', form.img)
  form.category = '图片'
}

// 多图上传成功回调
const handleMultiImgUploadSuccess = (res) => {
  console.log('多图上传成功 - 原始响应:', res)

  let imageUrl = ''
  // 检查响应格式
  if (typeof res === 'string') {
    imageUrl = res
  } else if (res && res.data) {
    imageUrl = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('图片上传响应格式错误')
    return
  }

  console.log('解析后的图URL:', imageUrl)
  imageList.value.push(imageUrl);
  form.category = '图片';

  // 如果没有封面，将第一张图设为封面
  if (!form.img && imageList.value.length > 0) {
    form.img = imageList.value[0];
  }

  console.log('当前图片列表:', imageList.value)
  console.log('imageList长度:', imageList.value.length)
}

// 删除图片
const handleRemoveImage = (index) => {
  imageList.value.splice(index, 1);
  // 如果删除的是封面图，更新封面
  if (imageList.value.length > 0) {
    form.img = imageList.value[0];
  } else {
    form.img = '';
  }
}

// 图片加载失败处理
const handleImageError = (event, img, index) => {
  console.error('图片加载失败:', img)
  ElMessage.error(`图片${index + 1}加载失败，请检查图片URL`)
}

// 上传失败处理
const handleUploadError = (error) => {
  console.error('上传失败:', error)
  ElMessage.error('图片上传失败，请重试')
}

// 视频上传成功回调
const handleVideoUploadSuccess = (res) => {
  console.log('视频上传成功 - 原始响应:', res)

  // 检查响应格式
  let videoUrl = ''
  if (typeof res === 'string') {
    videoUrl = res
  } else if (res && res.data) {
    videoUrl = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('视频上传响应格式错误')
    return
  }

  form.video = videoUrl
  console.log('视频URL:', form.video)
  form.category = '视频'
  form.img = ''
  imageList.value = []  // 清空多图列表

  nextTick(() => {
    generateVideoCoverPreview()
  })
}

// 视频上传失败处理
const handleVideoUploadError = (error) => {
  console.error('视频上传失败:', error)
  ElMessage.error('视频上传失败，请重试')
}

const types = ref([])
const loadType = () => {
  request.get('/type').then(res => {
    types.value = res.data
  })
}

// 生命周期钩子
onMounted(() => {
  loadType()
  // 默认设置为图片类型
  form.category = '图片'
})
</script>

<template>
  <div class="main-content">
    <el-card>
      <div class="title">创作服务平台</div>
      <el-form label-width="120px" size="small" style="width: 90%" :model="form" :rules="rules" ref="ruleFormRef">
        <el-tabs @tab-click="changeCategory">
          <el-tab-pane label="上传图片">
            <el-upload
                class="img-uploader"
                :action="`${serverHost}/web/upload`"
                :show-file-list="false"
                :on-success="handleImgUploadSuccess"
            >
              <img v-if="form.img" :src="form.img" class="img" alt="封面图">
              <el-icon v-else class="img-uploader-icon"><Plus /></el-icon>
            </el-upload>

            <!-- 多图上传区域 -->
            <div class="multi-upload-section">
              <div class="section-title">多图上传（可选）</div>
              <div class="image-list">
                <div v-for="(img, index) in imageList" :key="index" class="image-item">
                  <img
                      :src="img"
                      class="uploaded-image"
                      alt="上传图片"
                      @error="handleImageError($event, img, index)"
                  />
                  <div class="image-overlay">
                    <el-button type="danger" size="small" circle @click="handleRemoveImage(index)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-upload
                    :action="`${serverHost}/web/upload`"
                    :on-success="handleMultiImgUploadSuccess"
                    :on-error="handleUploadError"
                    :show-file-list="false"
                    accept="image/*"
                    class="upload-box"
                >
                  <div class="upload-trigger">
                    <el-icon size="30"><Plus /></el-icon>
                    <div class="upload-text">添加图片</div>
                  </div>
                </el-upload>
              </div>
              <div class="upload-tip">建议图片尺寸：800x600，支持jpg、png格式</div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="上传视频">
            <el-upload
                class="img-uploader"
                :action="`${serverHost}/web/upload`"
                :show-file-list="false"
                :on-success="handleVideoUploadSuccess"
                :on-error="handleVideoUploadError"
                accept="video/*"
            >
              <img v-if="videoCoverPreview" :src="videoCoverPreview" class="img">
              <el-icon v-else class="img-uploader-icon"><Plus /></el-icon>
            </el-upload>

            <div v-if="form.video" class="video-preview">
              <video ref="videoRef" controls :src="form.video" class="video-player"></video>
              <canvas ref="canvasRef" style="display: none"></canvas>
            </div>
          </el-tab-pane>
        </el-tabs>

        <el-form-item prop="name" label="标题">
          <el-input v-model="form.name" autocomplete="off" placeholder="好的创作值得一个好名字！"></el-input>
        </el-form-item>

        <el-form-item prop="typeId" label="分类">
          <el-select v-model="form.typeId" placeholder="请选择分类">
            <el-option
                v-for="item in types"
                :key="item.id"
                :label="item.name"
                :value="item.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item prop="content" label="内容">
          <div style="border: 1px solid #ccc; z-index: 100;">
            <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRefContent" :defaultConfig="editorConfig" mode="default" />
            <Editor style="height: 300px; overflow-y: hidden;" v-model="htmlContent" :defaultConfig="editorConfig" mode="default" @onCreated="editorRefContent = $event" />
          </div>
        </el-form-item>

        <!-- 显示当前类型（只读） -->
        <el-form-item label="类型">
          <el-tag v-if="form.video" type="primary">视频</el-tag>
          <el-tag v-else-if="imageList.length > 0" type="success">多图</el-tag>
          <el-tag v-else-if="form.img" type="info">图片</el-tag>
          <el-tag v-else type="warning">未设置</el-tag>
        </el-form-item>
      </el-form>

      <div style="margin-top: 10px;display: flex;justify-content: space-around">
        <el-button type="success" size="large" @click="save" :loading="isSubmitting">
          {{ isSubmitting ? '提交中...' : '立即投稿' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.main-content {
  display: flex;
  background-color: #fff;
  padding: 30px 20px;
  flex-direction: column;

  @media (max-width: 768px) {
    padding: 20px 10px;
  }

  .title {
    font-size: 25px;

    @media (max-width: 480px) {
      font-size: 20px;
    }

    &::after {
      content: '';
      width: 146px;
      height: 4px;
      display: block;
      border-radius: 10px;
    }

    .info {
      background-color: #eee;
      font-size: 16px;
      margin-bottom: 50px;
      width: 95%;
      border-radius: 10px;
      padding: 25px 10px 20px;

      .time {
        margin-bottom: 10px;
      }
    }

    .btns-wrap {
      display: flex;
      justify-content: space-between;
      margin-top: 20px;
      align-items: center;

      .el-button {
        height: 40px;
        width: 50%;
        font-size: 14px;
        border-radius: 50px;
      }
    }
  }
}

.img-uploader {
  text-align: center;
  padding-bottom: 10px;
}

:deep(.img-uploader .el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

:deep(.img-uploader .el-upload:hover) {
  border-color: #409EFF;
}

.img-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 350px;
  height: 280px;
  line-height: 280px;
  text-align: center;

  @media (max-width: 768px) {
    width: 250px;
    height: 200px;
    line-height: 200px;
  }

  @media (max-width: 480px) {
    width: 200px;
    height: 160px;
    line-height: 160px;
  }
}

.img {
  width: 350px;
  height: 280px;
  display: block;

  @media (max-width: 768px) {
    width: 250px;
    height: 200px;
  }

  @media (max-width: 480px) {
    width: 200px;
    height: 160px;
  }
}

.video-preview {
  width: 100%;
  max-width: 600px;
  margin: 20px auto;
  display: flex;
  justify-content: center;

  @media (max-width: 768px) {
    max-width: 100%;
  }

  .video-player {
    width: 100%;
    max-width: 500px;
    height: auto;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}

/* 多图上传样式 */
.multi-upload-section {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 15px;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #fff;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.upload-box {
  width: 120px;
  height: 120px;
}

.upload-trigger {
  width: 120px;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.upload-trigger:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-text {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  padding: 8px 0;
}
</style>