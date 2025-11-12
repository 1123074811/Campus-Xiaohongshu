package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.IFollowService;
import com.example.springboot.service.ILikeService;
import com.example.springboot.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;
    @Resource
    private IFollowService followService;
    @Resource
    private ICollectService collectService;
    @Resource
    private IBlogService blogService;
    @Resource
    private ILikeService likeService;

    @PostMapping
    public Result save(@RequestBody User user) {
        return Result.success(userService.saveOrUpdate(user));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(userService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(userService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(userService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable Integer id) {

        //查询所有关注量
        LambdaQueryWrapper<Follow> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Follow::getUserId, id);
        long followCount = followService.count(wrapper1);

        //查询所有粉丝量
        LambdaQueryWrapper<Follow> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Follow::getItemId, id);
        long followerCount = followService.count(wrapper2);

        //查询某个用户博客的被收藏量
        //首先查询哪些博客是用户发布的
        LambdaQueryWrapper<Blog> blogWrapper = new LambdaQueryWrapper<>();
        blogWrapper.eq(Blog::getUserId, id);

        List<Blog> blogs = blogService.list(blogWrapper);
        List<Collect> collects = collectService.list();
        List<Like> likes = likeService.list();

        int collectCount = 0;
        //遍历所有收藏信息
        for (Collect collect : collects) {
            //遍历所有博客,判断当前博客是否被收藏,如果被收藏则收藏量加1
            for (Blog blog : blogs) {
                if (Objects.equals(collect.getItemId(), blog.getId())) {
                    collectCount++;
                }
            }
        }

        int likeCount = 0;
        //遍历所有点赞信息
        for (Like like : likes) {
            //遍历所有博客,判断当前博客是否被点赞,如果被点赞则点赞量加1
            for (Blog blog : blogs) {
                if (Objects.equals(like.getItemId(), blog.getId())) {
                    likeCount++;
                }
            }
        }

        JSONObject object = new JSONObject();
        object.set("followCount", followCount);
        object.set("followerCount", followerCount);
        object.set("collectCount", collectCount);
        object.set("likeCount", likeCount);
        return Result.success(object);
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(User::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(User::getNickname, keyword);
        }

        return Result.success(userService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

