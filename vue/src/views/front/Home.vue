<script setup>
import {ref, onMounted, reactive, computed} from "vue";
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

const showBlog = (blog, event) => {
  console.log("点击博客：", blog);
};
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
    margin-left: 15px;
    border-radius: 20px;
  }
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

  &:hover .image-overlay {
    opacity: 1;
  }

  .item-body {
    margin: 9px;
  }

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
</style>