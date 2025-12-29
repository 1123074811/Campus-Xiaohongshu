<script setup>
import {ref, reactive, shallowRef} from 'vue'
import {Search, Plus, Delete, Edit, UploadFilled} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { serverHost } from '../../../config/config.default'
import request from '../../utils/request'
import axios from 'axios'

//引入富文本组件
import '@wangeditor/editor/dist/css/style.css'
import { Editor,Toolbar } from '@wangeditor/editor-for-vue'

// 表格数据
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 搜索条件
const searchForm = reactive({
  keyword: '',
})

// 表单数据
const form = ref({})
const dialogFormVisible = ref(false)
const multipleSelection = ref([])

// 加载数据
const load = () => {
  request.get("/blog/page", {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchForm.keyword,
    }
  }).then(res => {
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  })
}
load()

// 保存
const save = () => {
  form.value.content = htmlContent.value;
  
  // 根据上传内容自动设置 category
  if (form.value.video) {
    form.value.category = '视频'
  } else if (imageList.value.length > 0) {
    form.value.category = '多图'
    form.value.images = JSON.stringify(imageList.value)
    // 如果没有封面图，使用第一张图作为封面
    if (!form.value.img && imageList.value.length > 0) {
      form.value.img = imageList.value[0]
    }
  } else if (form.value.img) {
    form.value.category = '图片'
  }
  
  request.post("/blog", form.value).then(res => {
    if (res.code === '200') {
      ElMessage.success("保存成功")
      dialogFormVisible.value = false
      load()
    } else {
      ElMessage.error("保存失败")
    }
  })
}

// 添加
const handleAdd = () => {
  htmlContent.value = "";
  form.value = {}
  imageList.value = [];
  dialogFormVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  form.value = JSON.parse(JSON.stringify(row))
  htmlContent.value = form.value.content || '';
  
  // 加载多图数据
  imageList.value = []
  if (form.value.images) {
    try {
      const imagesArray = JSON.parse(form.value.images)
      imageList.value = [...imagesArray]
    } catch (e) {
      console.error('解析图片数据失败:', e)
      imageList.value = []
    }
  }
  
  dialogFormVisible.value = true
}

// 删除
const del = (id) => {
  request.delete("/blog/" + id).then(res => {
    if (res.code === '200') {
      ElMessage.success("删除成功")
      load()
    } else {
      ElMessage.error("删除失败")
    }
  })
}

// 批量删除
const delBatch = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  const ids = multipleSelection.value.map(v => v.id)
  request.post("/blog/del/batch", ids).then(res => {
    if (res.code === '200') {
      ElMessage.success("批量删除成功")
      load()
    } else {
      ElMessage.error("批量删除失败")
    }
  })
}

// 重置搜索
const reset = () => {
  searchForm.keyword = ""
  load()
}

// 表格选择变化
const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  load()
}

// 页码变化
const handleCurrentChange = (current) => {
  pageNum.value = current
  load()
}

// 确认删除
const confirmDelete = (id) => {
  ElMessageBox.confirm(
      '确定要删除这条数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        del(id)
      })
}

// 确认批量删除
const confirmBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning("请至少选择一条记录")
    return
  }

  ElMessageBox.confirm(
      '确定要批量删除这些数据吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        delBatch()
      })
}

// 图片上传
const handleImgUploadSuccess = (res) => {
  console.log('封面图上传成功 - 原始响应:', res)
  // 检查响应格式
  if (typeof res === 'string') {
    form.value.img = res
  } else if (res && res.data) {
    form.value.img = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('图片上传响应格式错误')
    return
  }
  console.log('封面图URL:', form.value.img)
};

// 多图上传
const imageList = ref([]);

const handleMultiImgUploadSuccess = (res) => {
  console.log('多图上传成功 - 原始响应:', res)
  
  let imageUrl = ''
  // 检查响应格式，兼容 string 和 object 两种格式
  if (typeof res === 'string') {
    imageUrl = res
  } else if (res && res.data) {
    imageUrl = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('图片上传响应格式错误')
    return
  }
  
  console.log('解析后的图片URL:', imageUrl)
  imageList.value.push(imageUrl);  // 确保push的是URL字符串
  form.value.images = JSON.stringify(imageList.value);
  console.log('当前图片列表:', imageList.value)
};

const handleRemoveImage = (index) => {
  imageList.value.splice(index, 1);
  form.value.images = imageList.value.length > 0 ? JSON.stringify(imageList.value) : '';
};

// 视频上传
const handleVideoUploadSuccess = (res) => {
  console.log('视频上传成功 - 原始响应:', res)
  // 检查响应格式
  if (typeof res === 'string') {
    form.value.video = res
  } else if (res && res.data) {
    form.value.video = res.data
  } else {
    console.error('未知的响应格式:', res)
    ElMessage.error('视频上传响应格式错误')
    return
  }
  console.log('视频URL:', form.value.video)
};

const types = ref([])
const loadTypes = () => {
  request.get("/type").then(res => {
    if (res.code === '200') {
      types.value = res.data
    }
  })
}
loadTypes()
const users = ref([])
const loadUsers = () => {
  request.get("/user").then(res => {
    if (res.code === '200') {
      users.value = res.data
    }
  })
}
loadUsers()

//定义富文本数据
const htmlContent = ref('');
const editorRefContent = shallowRef();

//wangEditor 配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: async (file, insertFn) => {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await axios({
            url: `${serverHost}/web/upload`,
            method: 'post',
            data: formData,
            headers: {'Content-Type': 'multipart/form-data'},
          })
          if (insertFn && typeof insertFn === 'function') {
            insertFn(res.data)
          }
        } catch (error) {
          console.error('上传失败:', error)
          ElMessage.error('上传失败')
        }
      },
    },
    uploadVideo: {
      customUpload: async (file, insertFn) => {
        const formData = new FormData()
        formData.append('file', file)
        try {
          const res = await axios({
            url: `${serverHost}/web/upload`,
            method: 'post',
            data: formData,
            headers: {'Content-Type': 'multipart/form-data'},
          })
          if (insertFn && typeof insertFn === 'function') {
            insertFn(res.data)
          }
        } catch (error) {
          console.error('上传失败:', error)
          ElMessage.error('上传失败')
        }
      },
    },
  }
}

const download = (url) => {
  window.open(url)
}

// 解析图片JSON数组
const parseImages = (imagesJson) => {
  if (!imagesJson) return []
  try {
    return JSON.parse(imagesJson)
  } catch (e) {
    console.error('解析图片数据失败:', e)
    return []
  }
}

// 计算总图片数量（包含封面）
const getTotalImageCount = (row) => {
  let count = 1  // 封面图（博客一定有封面）
  
  // 如果有多图数据，加上多图数量（去重封面）
  if (row.images) {
    const imagesArray = parseImages(row.images)
    // 过滤掉与封面相同的图片，避免重复计数
    const uniqueImages = imagesArray.filter(img => img !== row.img)
    count += uniqueImages.length
  }
  
  return count
}

const contentViewVisible = ref(false)
const currentViewContent = ref('');

const viewContent = (content) => {
  currentViewContent.value = content || '';
  contentViewVisible.value = true;
}

</script>

<template>
  <div class="content-container">

    <!-- 搜索和操作区域 -->
    <div class="action-bar">
      <div class="search-section">
        <el-input 
          v-model="searchForm.keyword" 
          placeholder="请输入博客标题" 
          class="search-input" 
          :prefix-icon="Search" 
          clearable
        />
        <el-button type="primary" @click="load" :icon="Search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-section">
        <el-button type="primary" @click="handleAdd" :icon="Plus">新增</el-button>
        <el-button type="danger" @click="confirmBatchDelete" :icon="Delete">批量删除</el-button>
      </div>
    </div>

    <!-- 表格区域 -->
    <el-card>
      <el-table :data="tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="60" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="博客标题" />
        <el-table-column prop="typeId" label="分类">
          <template #default="scope">
            {{types.find(item=> item.id === scope.row.typeId)?.name || '-'}}
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户">
          <template #default="scope">
            {{users.find(item=> item.id === scope.row.userId)?.nickname || '-'}}
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" />
        <el-table-column label="图片" width="100" align="center">
          <template #default="scope">
            <!-- 多图堆叠显示 -->
            <div v-if="scope.row.images" class="image-stack">
              <el-image 
                v-for="(img, index) in parseImages(scope.row.images).slice(0, 3)" 
                :key="index"
                :style="{ 
                  zIndex: 3 - index,
                  transform: `translateX(${index * 8}px) rotate(${index * 3}deg)`
                }"
                class="stacked-image" 
                :src="img" 
                :preview-src-list="parseImages(scope.row.images)" 
                :initial-index="index"
                :preview-teleported="true"
                fit="cover"
              />
              <!-- 图片数量标记（包含封面） -->
              <span class="image-count-badge">{{ getTotalImageCount(scope.row) }}</span>
            </div>
            <!-- 单图显示 -->
            <el-image 
              v-else-if="scope.row.img" 
              style="width: 60px; height: 60px; border-radius: 4px;" 
              :src="scope.row.img" 
              :preview-src-list="[scope.row.img]" 
              :preview-teleported="true"
              fit="cover"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column label="视频">
          <template #default="scope">
            <el-button type="primary" @click="download(scope.row.video)" v-if="scope.row.video">下载</el-button>
          </template>
        </el-table-column>

        <el-table-column label="类型">
          <template #default="scope">
            <el-tag type="primary">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="内容">
          <template #default="scope">
            <el-button type="primary" @click="viewContent(scope.row.content)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-tooltip content="编辑" placement="top" :effect="'light'">
              <el-button circle type="primary" :icon="Edit" @click="handleEdit(scope.row)"/>
            </el-tooltip>
            <el-tooltip content="删除" placement="top" :effect="'light'">
              <el-button circle type="danger" :icon="Delete" @click="confirmDelete(scope.row.id)"/>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-section">
        <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 表单对话框 -->
    <el-dialog v-model="dialogFormVisible" :title="form.id ? '编辑' : '新增'" width="60%" destroy-on-close center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="博客标题" required>
          <el-input v-model="form.name" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.typeId" placeholder="请选择分类">
            <el-option v-for="item in types" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户" required>
          <el-select v-model="form.userId" placeholder="请选择用户">
            <el-option v-for="item in users" :key="item.id" :label="item.nickname" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间" required>
          <el-date-picker v-model="form.time" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择日期时间"></el-date-picker>
        </el-form-item>
        <el-form-item label="封面" required>
          <div class="upload-container">
            <el-avatar v-if="form.img" :src="form.img" size="80" />
            <el-upload :action="`${serverHost}/web/upload`" :on-success="handleImgUploadSuccess" :show-file-list="false">
              <el-button type="primary" icon="UploadFilled">{{ form.img ? '更换图片' : '上传图片' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="多图上传">
          <div class="multi-upload-container">
            <div class="image-list">
              <div v-for="(img, index) in imageList" :key="index" class="image-item">
                <el-image :src="img" fit="cover" class="uploaded-image" :preview-src-list="imageList" :initial-index="index" />
                <div class="image-overlay">
                  <el-button type="danger" size="small" circle @click="handleRemoveImage(index)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <el-upload
                :action="`${serverHost}/web/upload`"
                :on-success="handleMultiImgUploadSuccess"
                :show-file-list="false"
                accept="image/*"
                class="upload-box"
              >
                <div class="upload-trigger">
                  <el-icon size="30"><Plus /></el-icon>
                  <div class="upload-text">上传图片</div>
                </div>
              </el-upload>
            </div>
            <div class="upload-tip">建议图片尺寸：800x600，支持jpg、png格式</div>
          </div>
        </el-form-item>
        <el-form-item label="视频">
          <div class="upload-container">
            <el-upload :action="`${serverHost}/web/upload`" :on-success="handleVideoUploadSuccess" :show-file-list="false">
              <el-button type="primary" :icon="UploadFilled">{{ form.video ? '更换视频' : '上传视频' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        
        <!-- 显示当前类型（只读） -->
        <el-form-item label="类型">
          <el-tag v-if="form.video" type="primary">视频</el-tag>
          <el-tag v-else-if="imageList.length > 0" type="success">多图</el-tag>
          <el-tag v-else-if="form.img" type="info">图片</el-tag>
          <el-tag v-else type="warning">未设置</el-tag>
        </el-form-item>
        <el-form-item label="内容" required>
          <div style="border: 1px solid #ccc; z-index: 100;">
            <Toolbar
                style="border-bottom: 1px solid #ccc"
                :editor="editorRefContent"
                :defaultConfig="editorConfig"
                mode="default"
            />
            <Editor
                style="height: 300px; overflow-y: hidden;"
                v-model="htmlContent"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="editorRefContent = $event"
            />
          </div>
        </el-form-item>

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="contentViewVisible" title="详情" width="60%" center>
      <div v-html="currentViewContent"></div>
    </el-dialog>
  </div>
</template>

<style scoped>
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.search-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 280px;
}

.toolbar-section {
  display: flex;
  gap: 12px;
}

.multi-upload-container {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.uploaded-image {
  width: 100%;
  height: 100%;
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
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-trigger:hover {
  border-color: #409eff;
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
}

/* 表格中的图片堆叠样式 */
.image-stack {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 60px;
}

.stacked-image {
  position: absolute;
  width: 60px;
  height: 60px;
  border-radius: 4px;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  cursor: pointer;
}

.stacked-image:hover {
  transform: translateY(-5px) scale(1.1) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  z-index: 10 !important;
}

.image-count-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  padding: 3px 7px;
  border-radius: 12px;
  z-index: 5;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  border: 2px solid #fff;
  min-width: 20px;
  text-align: center;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .search-section {
    flex-wrap: wrap;
  }
  
  .search-input {
    width: 100%;
  }
  
  .toolbar-section {
    justify-content: flex-start;
  }
}
</style>