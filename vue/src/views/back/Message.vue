<script setup>
import {ref, reactive, shallowRef} from 'vue'
import {Search, Plus, Delete, Edit, UploadFilled} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

//引入富文本组件
import '@wangeditor/editor/dist/css/style.css'

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
const multipleSelection = ref([])

// 加载数据
const load = () => {
  request.get("/message/page", {
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

// 删除
const del = (id) => {
  request.delete("/message/" + id).then(res => {
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
  request.post("/message/del/batch", ids).then(res => {
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

const items = ref([])
const loadItems = () => {
  request.get("/blog").then(res => {
    if (res.code === '200') {
      items.value = res.data
    }
  })
}
loadItems()
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
const contentViewVisible = ref(false)
const currentViewContent = ref('');

</script>

<template>
  <div class="content-container">

    <!-- 搜索和操作区域 -->
    <div class="action-bar">
      <div class="search-section">
        <el-input 
          v-model="searchForm.keyword" 
          placeholder="请输入内容" 
          class="search-input" 
          :prefix-icon="Search" 
          clearable
        />
        <el-button type="primary" @click="load" :icon="Search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="toolbar-section">
        <el-button type="danger" @click="confirmBatchDelete" :icon="Delete">批量删除</el-button>
      </div>
    </div>

    <!-- 表格区域 -->
    <el-card>
      <el-table :data="tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="60" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="text" label="通知内容"/>
        <el-table-column prop="type" label="通知类型"/>
        <el-table-column prop="time" label="通知时间"/>
        <el-table-column prop="fromUserId" label="发送通知的用户ID"/>
        <el-table-column prop="toUserId" label="接收通知的用户ID"/>
        <el-table-column prop="itemId" label="关联的博客ID"/>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
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