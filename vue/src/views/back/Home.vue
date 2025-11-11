<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts';
import request from "@/utils/request.js";

const users = ref([])
const blogs = ref([])
const loadBlogs = () => {
  request.get('/blog').then(res => {
    if (res.data) {
      blogs.value = res.data
    }
  })
}
const loadUsers = () => {
  request.get('/user').then(res => {
    if (res.data) {
      users.value = res.data
    }
  })
}
loadUsers()
loadBlogs()

onMounted(() => {

  const chartDom = document.getElementById('main');
  const myChart = echarts.init(chartDom);
  const option = {
    title: {
      text: '统计不同分类博客数量',
      subtext: '饼图',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: 'Access From',
        type: 'pie',
        radius: '50%',
        data: [],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };

  request.get('/blog/count').then(res=>{
    option.series[0].data = res.data
    option && myChart.setOption(option);
  })

})


</script>

<template>
  <div style="display: flex; gap: 30px">
    <el-card style="flex: 1">
      用户数量：{{users.length}}
    </el-card>
    <el-card style="flex: 1">
      博客数量：{{blogs.length}}
    </el-card>
  </div>

  <div style="display: flex; gap: 30px">
    <div style="width: 100%; height: 600px; margin-top: 20px;" ref="main" id="main"></div>
    <div style="width: 100%; height: 600px; margin-top: 20px;" ></div>
  </div>
</template>

<style scoped>
</style>