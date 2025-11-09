package com.example.springboot.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Collect;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.apache.el.parser.Token;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private ICollectService collectService;

    @PostMapping
    public Result save(@RequestBody Blog blog) {
        return Result.success(blogService.saveOrUpdate(blog));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        return Result.success(blogService.removeById(id));
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        return Result.success(blogService.removeByIds(ids));
    }

    @GetMapping
    public Result findAll() {
        return Result.success(blogService.list());
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(blogService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        return Result.success(blogService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @GetMapping("/front/page")
    public Result findFrontPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam Integer typeId,
                           @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        if (typeId != 0){
            queryWrapper.eq(Blog::getTypeId, typeId);
        }

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        //拿到所有收藏信息
        List<Collect> collects = collectService.list();

        Account account = TokenUtils.getCurrentUser();

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);
        for (Blog blog : page.getRecords()){
            //对于每个博客来说，都应该统计收藏记录
            //1.这个博客被收藏的次数
            //2.这博客是否被当前用户收藏
            int count = 0;
            for (Collect collect : collects) {
                //首先判断这个收藏信息是否和当前博客匹配，如果是，说明被收藏一次，则count加1
                if (Objects.equals(collect.getItemId(), blog.getId())){  //Integer类型的比较：Objects.equals()
                    count++;
                    //如果执行到这里，判断这个博客是否被当前用户收藏了
                    if (Objects.equals(collect.getUserId(), account.getId())){
                        blog.setIsCollected(true);
                    }
                }
            }
            blog.setCount(count);
        }

        return Result.success(page);
    }

    @GetMapping("/user/page")
    public Result findUserPage(@RequestParam Integer pageNum,
                                @RequestParam Integer pageSize,
                                @RequestParam Integer userId,
                                @RequestParam(defaultValue = "") String keyword) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Blog::getUserId, userId);
        queryWrapper.orderByDesc(Blog::getId);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        //拿到所有收藏信息
        List<Collect> collects = collectService.list();

        Account account = TokenUtils.getCurrentUser();

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);
        for (Blog blog : page.getRecords()){
            //对于每个博客来说，都应该统计收藏记录
            //1.这个博客被收藏的次数
            //2.这博客是否被当前用户收藏
            int count = 0;
            for (Collect collect : collects) {
                //首先判断这个收藏信息是否和当前博客匹配，如果是，说明被收藏一次，则count加1
                if (Objects.equals(collect.getItemId(), blog.getId())){  //Integer类型的比较：Objects.equals()
                    count++;
                    //如果执行到这里，判断这个博客是否被当前用户收藏了
                    if (Objects.equals(collect.getUserId(), account.getId())){
                        blog.setIsCollected(true);
                    }
                }
            }
            blog.setCount(count);
        }

        return Result.success(page);
    }

    @GetMapping("/collect/page")
    public Result findCollectPage(@RequestParam Integer pageNum,
                                @RequestParam Integer pageSize,
                                @RequestParam Integer userId,
                                @RequestParam(defaultValue = "") String keyword) {

        //拿到所有收藏信息
        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId);
        List<Collect> collects = collectService.list(wrapper);
        if (CollectionUtil.isEmpty(collects)) return Result.success(collectService.page(new Page<>(pageNum, pageSize), wrapper));

        List<Integer> ids = new ArrayList<>();
        for (Collect collect : collects) {
            ids.add(collect.getItemId());
        }

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Blog::getId);

        queryWrapper.in(Blog::getId, ids);

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getName, keyword);
        }

        Account account = TokenUtils.getCurrentUser();

        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);
        for (Blog blog : page.getRecords()){
            //对于每个博客来说，都应该统计收藏记录
            //1.这个博客被收藏的次数
            //2.这博客是否被当前用户收藏
            int count = 0;
            for (Collect collect : collects) {
                //首先判断这个收藏信息是否和当前博客匹配，如果是，说明被收藏一次，则count加1
                if (Objects.equals(collect.getItemId(), blog.getId())){  //Integer类型的比较：Objects.equals()
                    count++;
                    //如果执行到这里，判断这个博客是否被当前用户收藏了
                    if (Objects.equals(collect.getUserId(), account.getId())){
                        blog.setIsCollected(true);
                    }
                }
            }
            blog.setCount(count);
        }

        return Result.success(page);
    }
}

