package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Message;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.IMessageService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/collect")
public class CollectController {

    @Resource
    private ICollectService collectService;
    @Resource
    private IMessageService messageService;
    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody Collect collect) {

        Account account = TokenUtils.getCurrentUser();

        collect.setUserId(account.getId());

        try {
            collectService.saveOrUpdate(collect);

            try {
                Message message = new Message();
                message.setText("收藏了你的笔记！");
                message.setType("收藏");
                message.setTime(DateUtil.now());
                message.setFromUserId(account.getId());
                message.setItemId(collect.getItemId());
                Blog blog = blogService.getById(collect.getItemId());
                if (blog != null) {
                    message.setToUserId(blog.getUserId());
                    messageService.save(message);
                }
            } catch (Exception e) {
                // 消息保存失败不应该影响收藏功能
                e.printStackTrace();
            }

        } catch (Exception e) {
            //在收藏表中设置索引，如果报错了，说明该用户已经收藏了该博客，则将则条收藏删掉，相当于点击一次收藏，再点击取消收藏
            LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Collect::getUserId, account.getId());     //Collect::getUserId是前端传的userId，account.getId()是当前登录用户的id
            queryWrapper.eq(Collect::getItemId, collect.getItemId());  //Collect::getItemId是前端传的itemId，collect.getItemId()是当前收藏的博客id（即数据库中的itemId）
            collectService.remove(queryWrapper);
            return Result.error("605", "取消收藏成功！");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(collectService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(collectService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(collectService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(collectService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Collect::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Collect::getItemId, keyword);
        }

        return Result.success(collectService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }


}

