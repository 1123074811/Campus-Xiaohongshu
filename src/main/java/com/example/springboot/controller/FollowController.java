package com.example.springboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Follow;
import com.example.springboot.entity.Message;
import com.example.springboot.service.IFollowService;
import com.example.springboot.service.IMessageService;
import com.example.springboot.service.IUserService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    @Resource
    private IMessageService messageService;
    @Resource
    private IUserService userService;

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

            try {
                Message message = new Message();
                message.setText("关注了你！");
                message.setType("关注");
                message.setTime(DateUtil.now());
                message.setFromUserId(account.getId());
                message.setToUserId(follow.getItemId());
                messageService.save(message);
            } catch (Exception e) {
                // 消息保存失败不应该影响收藏功能
                e.printStackTrace();
            }
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

    @GetMapping("/friend/list")
    public Result friendList() {
        Account account = TokenUtils.getCurrentUser();
        if (account == null || account.getId() == null) {
            return Result.error("401", "未登录");
        }
        LambdaQueryWrapper<Follow> myFollows = new LambdaQueryWrapper<>();
        myFollows.eq(Follow::getUserId, account.getId());
        List<Follow> iFollow = followService.list(myFollows);
        List<Integer> candidates = iFollow.stream().map(Follow::getItemId).toList();
        if (candidates.isEmpty()) {
            return Result.success(List.of());
        }
        LambdaQueryWrapper<Follow> reverse = new LambdaQueryWrapper<>();
        reverse.in(Follow::getUserId, candidates).eq(Follow::getItemId, account.getId());
        List<Follow> mutual = followService.list(reverse);
        Set<Integer> friendIds = new HashSet<>();
        for (Follow f : mutual) friendIds.add(f.getUserId());
        List<Object> friends = new ArrayList<>();
        for (Integer fid : friendIds) {
            var u = userService.getById(fid);
            if (u != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("user_id", u.getId());
                m.put("nickname", u.getNickname());
                m.put("avatar_url", u.getAvatarUrl());
                friends.add(m);
            }
        }
        return Result.success(Map.of("friends", friends));
    }

    @GetMapping("/friend/status")
    public Result friendStatus(@RequestParam("user_id") Integer userId) {
        Long last = ChatController.getLastActive(userId);
        String status = "offline";
        if (last != null && System.currentTimeMillis() - last <= 90_000) {
            status = "online";
        }
        return Result.success(Map.of("status", status, "last_active_at", last));
    }


}

