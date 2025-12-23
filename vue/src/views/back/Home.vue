<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts';
import request from "@/utils/request.js";
import {
  User,
  Document,
  ChatDotRound,
  Star,
  Collection,
  TrendCharts
} from '@element-plus/icons-vue'

// 基础数据
const users = ref([])
const blogs = ref([])
const overview = ref({
  totalUsers: 0,
  totalBlogs: 0,
  totalComments: 0,
  totalLikes: 0,
  totalCollects: 0,
  todayBlogs: 0,
  todayComments: 0
})

// 时间范围选择
const timeRange = ref('day')
const dataLimit = ref(30)

// 图表实例
let categoryChart = null
let trendChart = null
let activityChart = null
let interactionChart = null

// 加载基础数据
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

// 加载总体概览数据
const loadOverview = () => {
  request.get('/admin/dashboard/overview').then(res => {
    if (res.data) {
      overview.value = res.data
    }
  })
}

// 初始化博客分类分布饼图
const initCategoryChart = () => {
  const chartDom = document.getElementById('categoryChart');
  if (!chartDom) return;

  categoryChart = echarts.init(chartDom);
  const option = {
    title: {
      text: '博客分类分布',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [
      {
        name: '博客分类',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}: {c}'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        data: []
      }
    ],
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
  };

  request.get('/admin/dashboard/blog/category').then(res => {
    if (res.data) {
      option.series[0].data = res.data
      categoryChart.setOption(option);
    }
  })
}

// 初始化博客发布趋势图
const initTrendChart = () => {
  const chartDom = document.getElementById('trendChart');
  if (!chartDom) return;

  trendChart = echarts.init(chartDom);
  loadTrendData();
}

// 加载趋势数据
const loadTrendData = () => {
  request.get('/admin/dashboard/blog/trend', {
    params: {
      timeRange: timeRange.value,
      limit: dataLimit.value
    }
  }).then(res => {
    if (res.data) {
      const dates = res.data.map(item => item.date)
      const counts = res.data.map(item => item.count)

      const option = {
        title: {
          text: '博客发布趋势',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '发布数量'
        },
        series: [
          {
            name: '博客数量',
            type: 'line',
            smooth: true,
            data: counts,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(84, 112, 198, 0.5)' },
                { offset: 1, color: 'rgba(84, 112, 198, 0.1)' }
              ])
            },
            itemStyle: {
              color: '#5470c6'
            },
            lineStyle: {
              width: 3
            }
          }
        ]
      };

      if (trendChart) {
        trendChart.setOption(option);
      }
    }
  })
}

// 初始化用户活跃度图表
const initActivityChart = () => {
  const chartDom = document.getElementById('activityChart');
  if (!chartDom) return;

  activityChart = echarts.init(chartDom);
  loadActivityData();
}

// 加载活跃度数据
const loadActivityData = () => {
  request.get('/admin/dashboard/user/activity', {
    params: {
      timeRange: timeRange.value,
      limit: dataLimit.value
    }
  }).then(res => {
    if (res.data) {
      const dates = res.data.map(item => item.date)
      const activeUsers = res.data.map(item => item.activeUsers)

      const option = {
        title: {
          text: '用户活跃度统计',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '活跃用户数'
        },
        series: [
          {
            name: '活跃用户',
            type: 'bar',
            data: activeUsers,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#91cc75' },
                { offset: 1, color: '#5daf34' }
              ]),
              borderRadius: [5, 5, 0, 0]
            },
            barWidth: '60%'
          }
        ]
      };

      if (activityChart) {
        activityChart.setOption(option);
      }
    }
  })
}

// 初始化互动数据图表
const initInteractionChart = () => {
  const chartDom = document.getElementById('interactionChart');
  if (!chartDom) return;

  interactionChart = echarts.init(chartDom);
  loadInteractionData();
}

// 加载互动数据
const loadInteractionData = () => {
  request.get('/admin/dashboard/interaction/stats', {
    params: {
      timeRange: timeRange.value,
      limit: dataLimit.value
    }
  }).then(res => {
    if (res.data) {
      const dates = res.data.map(item => item.date)
      const comments = res.data.map(item => item.comments)

      const option = {
        title: {
          text: '互动数据统计',
          left: 'center',
          textStyle: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['评论'],
          top: 'bottom'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '数量'
        },
        series: [
          {
            name: '评论',
            type: 'line',
            smooth: true,
            data: comments,
            itemStyle: {
              color: '#ee6666'
            },
            lineStyle: {
              width: 2
            }
          }
        ]
      };

      if (interactionChart) {
        interactionChart.setOption(option);
      }
    }
  })
}

// 时间范围变化处理
const handleTimeRangeChange = () => {
  // 根据时间范围调整数据点数量
  if (timeRange.value === 'month') {
    dataLimit.value = 12
  } else if (timeRange.value === 'week') {
    dataLimit.value = 12
  } else {
    dataLimit.value = 30
  }

  // 重新加载所有图表数据
  loadTrendData()
  loadActivityData()
  loadInteractionData()
}

// 窗口大小变化时调整图表
const handleResize = () => {
  categoryChart?.resize()
  trendChart?.resize()
  activityChart?.resize()
  interactionChart?.resize()
}

// 初始化
loadUsers()
loadBlogs()
loadOverview()

onMounted(() => {
  nextTick(() => {
    initCategoryChart()
    initTrendChart()
    initActivityChart()
    initInteractionChart()

    window.addEventListener('resize', handleResize)
  })
})

// 组件卸载时清理
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  categoryChart?.dispose()
  trendChart?.dispose()
  activityChart?.dispose()
  interactionChart?.dispose()
})
</script>

<template>
  <div class="dashboard-container">
    <!-- 数据概览卡片 -->
    <div class="overview-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
            <el-icon :size="30"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ overview.totalUsers }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
            <el-icon :size="30"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总博客数</div>
            <div class="stat-value">{{ overview.totalBlogs }}</div>
            <div class="stat-badge">今日 +{{ overview.todayBlogs }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
            <el-icon :size="30"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总评论数</div>
            <div class="stat-value">{{ overview.totalComments }}</div>
            <div class="stat-badge">今日 +{{ overview.todayComments }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
            <el-icon :size="30"><Star /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总点赞数</div>
            <div class="stat-value">{{ overview.totalLikes }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" style="background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);">
            <el-icon :size="30"><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总收藏数</div>
            <div class="stat-value">{{ overview.totalCollects }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 时间范围选择器 -->
    <div class="time-range-selector">
      <el-radio-group v-model="timeRange" @change="handleTimeRangeChange">
        <el-radio-button value="day">按天</el-radio-button>
        <el-radio-button value="week">按周</el-radio-button>
        <el-radio-button value="month">按月</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card class="chart-card" shadow="hover">
        <div id="trendChart" class="chart-container"></div>
      </el-card>
      <el-card class="chart-card" shadow="hover">
        <div id="activityChart" class="chart-container"></div>
      </el-card>
    </div>

    <div class="charts-row">
      <el-card class="chart-card" shadow="hover">
        <div id="categoryChart" class="chart-container"></div>
      </el-card>
      <el-card class="chart-card" shadow="hover">
        <div id="interactionChart" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 概览卡片样式 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  transition: all 0.3s ease;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-badge {
  display: inline-block;
  margin-top: 5px;
  padding: 2px 8px;
  background: #67c23a;
  color: white;
  font-size: 12px;
  border-radius: 10px;
}

/* 时间范围选择器 */
.time-range-selector {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

/* 图表布局 */
.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  transition: all 0.3s ease;
}

.chart-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chart-container {
  width: 100%;
  height: 400px;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-container {
    height: 300px;
  }
}

@media (max-width: 480px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
}
</style>