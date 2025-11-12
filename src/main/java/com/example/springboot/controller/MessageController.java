package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Message;
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
@RequestMapping("/message")
public class MessageController {

    @Resource
    private IMessageService messageService;

    @PostMapping
    public Result save(@RequestBody Message message) {
        return Result.success(messageService.saveOrUpdate(message));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(messageService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(messageService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getToUserId, TokenUtils.getCurrentUser().getId());
        // 按时间降序排序，最近的消息显示在前面
        queryWrapper.orderByDesc(Message::getTime);
        return Result.success(messageService.list(queryWrapper));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(messageService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Message::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Message::getText, keyword);
        }

        return Result.success(messageService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

}

