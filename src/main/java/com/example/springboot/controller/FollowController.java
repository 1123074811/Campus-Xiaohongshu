package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Follow;
import com.example.springboot.service.IFollowService;
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
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PostMapping
    public Result save(@RequestBody Follow follow) {

        Account account = TokenUtils.getCurrentUser();

        //如果是关注自己，需要拦截一下
        if (Objects.equals(account.getId(), follow.getItemId())){
            return Result.error("605", "不能关注自己！");
        }

        follow.setUserId(account.getId());

        try {
            followService.saveOrUpdate(follow);
        } catch (Exception e) {
            LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Follow::getUserId, account.getId());
            queryWrapper.eq(Follow::getItemId, follow.getItemId());
            followService.remove(queryWrapper);
            return Result.error("605", "取消关注成功！");
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(followService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(followService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(followService.list());
    }

    @GetMapping("/checkFollow/{id}")
    public Result checkFollow(@PathVariable Integer id) {
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getUserId, TokenUtils.getCurrentUser().getId());
        queryWrapper.eq(Follow::getItemId, id);
        return followService.count(queryWrapper) > 0 ? Result.success():Result.error("605", "未关注！");
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(followService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Follow::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Follow::getItemId, keyword);
        }

        return Result.success(followService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }


}

