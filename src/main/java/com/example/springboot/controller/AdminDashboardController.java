package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.service.*;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 后台仪表盘统计控制器
 */
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    @Resource
    private IBlogService blogService;
    
    @Resource
    private IUserService userService;
    
    @Resource
    private ICommentService commentService;
    
    @Resource
    private ILikeService likeService;
    
    @Resource
    private ICollectService collectService;
    
    @Resource
    private ITypeService typeService;

    /**
     * 获取博客发布趋势数据
     * @param timeRange 时间范围：day-按天，week-按周，month-按月
     * @param limit 返回数据点数量，默认30
     */
    @GetMapping("/blog/trend")
    public Result getBlogTrend(@RequestParam(defaultValue = "day") String timeRange,
                               @RequestParam(defaultValue = "30") Integer limit) {
        List<Blog> blogs = blogService.list();
        
        JSONArray result = new JSONArray();
        SimpleDateFormat sdf;
        Calendar calendar = Calendar.getInstance();
        
        // 根据时间范围设置格式和日期处理
        if ("month".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-MM");
            calendar.add(Calendar.MONTH, -limit);
        } else if ("week".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-ww");
            calendar.add(Calendar.WEEK_OF_YEAR, -limit);
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            calendar.add(Calendar.DAY_OF_YEAR, -limit);
        }
        
        // 生成时间序列和统计数据
        Map<String, Integer> countMap = new LinkedHashMap<>();
        Date startDate = calendar.getTime();
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(startDate);
        
        for (int i = 0; i < limit; i++) {
            String dateStr = sdf.format(tempCal.getTime());
            countMap.put(dateStr, 0);
            
            if ("month".equals(timeRange)) {
                tempCal.add(Calendar.MONTH, 1);
            } else if ("week".equals(timeRange)) {
                tempCal.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                tempCal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        
        // 统计每个时间段的博客数量
        for (Blog blog : blogs) {
            if (blog.getTime() != null) {
                try {
                    Date blogDate = DateUtil.parse(blog.getTime());
                    if (blogDate.after(startDate) || blogDate.equals(startDate)) {
                        String dateStr = sdf.format(blogDate);
                        if (countMap.containsKey(dateStr)) {
                            countMap.put(dateStr, countMap.get(dateStr) + 1);
                        }
                    }
                } catch (Exception e) {
                    // 忽略日期解析错误
                }
            }
        }
        
        // 转换为JSON格式
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.set("date", entry.getKey());
            obj.set("count", entry.getValue());
            result.add(obj);
        }
        
        return Result.success(result);
    }

    /**
     * 获取用户活跃度统计
     * @param timeRange 时间范围：day-按天，week-按周，month-按月
     * @param limit 返回数据点数量
     */
    @GetMapping("/user/activity")
    public Result getUserActivity(@RequestParam(defaultValue = "day") String timeRange,
                                  @RequestParam(defaultValue = "30") Integer limit) {
        List<Blog> blogs = blogService.list();
        List<Comment> comments = commentService.list();
        List<Like> likes = likeService.list();
        
        JSONArray result = new JSONArray();
        SimpleDateFormat sdf;
        Calendar calendar = Calendar.getInstance();
        
        if ("month".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-MM");
            calendar.add(Calendar.MONTH, -limit);
        } else if ("week".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-ww");
            calendar.add(Calendar.WEEK_OF_YEAR, -limit);
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            calendar.add(Calendar.DAY_OF_YEAR, -limit);
        }
        
        Map<String, Set<Integer>> activeUsersMap = new LinkedHashMap<>();
        Date startDate = calendar.getTime();
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(startDate);
        
        for (int i = 0; i < limit; i++) {
            String dateStr = sdf.format(tempCal.getTime());
            activeUsersMap.put(dateStr, new HashSet<>());
            
            if ("month".equals(timeRange)) {
                tempCal.add(Calendar.MONTH, 1);
            } else if ("week".equals(timeRange)) {
                tempCal.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                tempCal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        
        // 统计发布博客的用户
        for (Blog blog : blogs) {
            if (blog.getTime() != null && blog.getUserId() != null) {
                try {
                    Date blogDate = DateUtil.parse(blog.getTime());
                    if (blogDate.after(startDate) || blogDate.equals(startDate)) {
                        String dateStr = sdf.format(blogDate);
                        if (activeUsersMap.containsKey(dateStr)) {
                            activeUsersMap.get(dateStr).add(blog.getUserId());
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }
        
        // 统计发表评论的用户
        for (Comment comment : comments) {
            if (comment.getTime() != null && comment.getUserId() != null) {
                try {
                    Date commentDate = DateUtil.parse(comment.getTime());
                    if (commentDate.after(startDate) || commentDate.equals(startDate)) {
                        String dateStr = sdf.format(commentDate);
                        if (activeUsersMap.containsKey(dateStr)) {
                            activeUsersMap.get(dateStr).add(comment.getUserId());
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }
        
        // 转换为JSON格式
        for (Map.Entry<String, Set<Integer>> entry : activeUsersMap.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.set("date", entry.getKey());
            obj.set("activeUsers", entry.getValue().size());
            result.add(obj);
        }
        
        return Result.success(result);
    }

    /**
     * 获取博客分类分布统计
     */
    @GetMapping("/blog/category")
    public Result getBlogCategory() {
        List<Type> typeList = typeService.list();
        List<Blog> blogList = blogService.list();
        
        JSONArray result = new JSONArray();
        
        for (Type type : typeList) {
            int count = 0;
            for (Blog blog : blogList) {
                if (Objects.equals(blog.getTypeId(), type.getId())) {
                    count++;
                }
            }
            JSONObject obj = new JSONObject();
            obj.set("name", type.getName());
            obj.set("value", count);
            result.add(obj);
        }
        
        return Result.success(result);
    }

    /**
     * 获取互动数据统计
     * @param timeRange 时间范围：day-按天，week-按周，month-按月
     * @param limit 返回数据点数量
     */
    @GetMapping("/interaction/stats")
    public Result getInteractionStats(@RequestParam(defaultValue = "day") String timeRange,
                                      @RequestParam(defaultValue = "30") Integer limit) {
        List<Comment> comments = commentService.list();
        List<Like> likes = likeService.list();
        List<Collect> collects = collectService.list();
        
        JSONArray result = new JSONArray();
        SimpleDateFormat sdf;
        Calendar calendar = Calendar.getInstance();
        
        if ("month".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-MM");
            calendar.add(Calendar.MONTH, -limit);
        } else if ("week".equals(timeRange)) {
            sdf = new SimpleDateFormat("yyyy-ww");
            calendar.add(Calendar.WEEK_OF_YEAR, -limit);
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd");
            calendar.add(Calendar.DAY_OF_YEAR, -limit);
        }
        
        Map<String, int[]> statsMap = new LinkedHashMap<>();
        Date startDate = calendar.getTime();
        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(startDate);
        
        for (int i = 0; i < limit; i++) {
            String dateStr = sdf.format(tempCal.getTime());
            statsMap.put(dateStr, new int[3]); // [评论数, 点赞数, 收藏数]
            
            if ("month".equals(timeRange)) {
                tempCal.add(Calendar.MONTH, 1);
            } else if ("week".equals(timeRange)) {
                tempCal.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                tempCal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        
        // 统计评论
        for (Comment comment : comments) {
            if (comment.getTime() != null) {
                try {
                    Date commentDate = DateUtil.parse(comment.getTime());
                    if (commentDate.after(startDate) || commentDate.equals(startDate)) {
                        String dateStr = sdf.format(commentDate);
                        if (statsMap.containsKey(dateStr)) {
                            statsMap.get(dateStr)[0]++;
                        }
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }
        
        // 转换为JSON格式
        for (Map.Entry<String, int[]> entry : statsMap.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.set("date", entry.getKey());
            obj.set("comments", entry.getValue()[0]);
            obj.set("likes", likes.size()); // 由于Like和Collect没有时间字段，只能显示总数
            obj.set("collects", collects.size());
            result.add(obj);
        }
        
        return Result.success(result);
    }

    /**
     * 获取总体统计概览
     */
    @GetMapping("/overview")
    public Result getOverview() {
        long userCount = userService.count();
        long blogCount = blogService.count();
        long commentCount = commentService.count();
        long likeCount = likeService.count();
        long collectCount = collectService.count();
        
        // 计算今日新增
        String today = DateUtil.today();
        
        LambdaQueryWrapper<Blog> blogWrapper = new LambdaQueryWrapper<>();
        blogWrapper.like(Blog::getTime, today);
        long todayBlogs = blogService.count(blogWrapper);
        
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.like(Comment::getTime, today);
        long todayComments = commentService.count(commentWrapper);
        
        JSONObject result = new JSONObject();
        result.set("totalUsers", userCount);
        result.set("totalBlogs", blogCount);
        result.set("totalComments", commentCount);
        result.set("totalLikes", likeCount);
        result.set("totalCollects", collectCount);
        result.set("todayBlogs", todayBlogs);
        result.set("todayComments", todayComments);
        
        return Result.success(result);
    }
}
