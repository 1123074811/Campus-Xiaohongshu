package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.Blog;
import com.example.springboot.service.IBlogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        Page<Blog> page = blogService.page(new Page<>(pageNum, pageSize), queryWrapper);
        for (Blog blog : page.getRecords()){
            blog.setCount(0);
            blog.setIsCollected(true);
        }

        return Result.success(page);
    }

}

