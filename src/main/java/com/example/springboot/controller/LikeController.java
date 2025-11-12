package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Like;
import com.example.springboot.entity.Message;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ILikeService;
import com.example.springboot.service.IMessageService;
import com.example.springboot.utils.TokenUtils;
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
@RequestMapping("/like")
public class LikeController {

    @Resource
    private ILikeService likeService;
    @Resource
    private IMessageService messageService;
    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody Like like) {

        Account account = TokenUtils.getCurrentUser();

        like.setUserId(account.getId());

        try {
            likeService.saveOrUpdate(like);

            try {
                Message message = new Message();
                message.setText("点赞了你的笔记！");
                message.setType("点赞");
                message.setTime(DateUtil.now());
                message.setFromUserId(account.getId());
                message.setItemId(like.getItemId());
                Blog blog = blogService.getById(like.getItemId());
                if (blog != null && !Objects.equals(account.getId(), blog.getUserId())) {
                    // 只有当操作用户不是blog作者时才发送消息
                    message.setToUserId(blog.getUserId());
                    messageService.save(message);
                }
            } catch (Exception e) {
                // 消息保存失败不应该影响点赞功能
                e.printStackTrace();
            }

        } catch (Exception e) {
            //在点赞表中设置索引，如果报错了，说明该用户已经点赞了该博客，则将则条点赞删掉，相当于点击一次点赞，再点击取消点赞
            LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Like::getUserId, account.getId());     //Like::getUserId是前端传的userId，account.getId()是当前登录用户的id
            queryWrapper.eq(Like::getItemId, like.getItemId());  //Like::getItemId是前端传的itemId，like.getItemId()是当前点赞的博客id（即数据库中的itemId）
            likeService.remove(queryWrapper);
            return Result.error("605", "取消点赞成功！");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(likeService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(likeService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(likeService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(likeService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Like::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Like::getItemId, keyword);
        }

        return Result.success(likeService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }


}

