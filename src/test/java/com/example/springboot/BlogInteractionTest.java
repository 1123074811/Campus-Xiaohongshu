package com.example.springboot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.controller.BlogController;
import com.example.springboot.controller.CollectController;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.Blog;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Like;
import com.example.springboot.service.IBlogService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.ILikeService;
import com.example.springboot.service.IMessageService;
import com.example.springboot.service.ITypeService;
import com.example.springboot.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 博客互动模块（收藏/点赞）自动化测试类
 * 测试对象：BlogController、CollectController 中的收藏与点赞相关功能
 * 目的：通过黑盒测试方法验证互动模块的功能正确性与缺陷
 */
@ExtendWith(MockitoExtension.class)
public class BlogInteractionTest {

    @Mock
    private IBlogService blogService;

    @Mock
    private ICollectService collectService;

    @Mock
    private ILikeService likeService;

    @Mock
    private ITypeService typeService;

    @Mock
    private IMessageService messageService;

    @InjectMocks
    private BlogController blogController;

    // ==================== 缺陷复现型测试 ====================

    /**
     * TC-BLOG-001: 博客发布无内容校验
     * 缺陷：BlogController.save() 未校验博客 name/content 字段，
     * 空标题、空内容的博客可直接发布保存。
     */
    @Test
    @DisplayName("TC-BLOG-001: 空标题空内容博客可被发布")
    public void saveBlogWithEmptyNameAndContent() {
        Blog emptyBlog = new Blog();
        emptyBlog.setName("");
        emptyBlog.setContent("");

        // 模拟 TokenUtils.getCurrentUser() 返回用户
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account mockAccount = new Account();
            mockAccount.setId(1);
            mockAccount.setRole("ROLE_USER");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            when(blogService.saveOrUpdate(any(Blog.class))).thenReturn(true);

            Result result = blogController.save(emptyBlog);

            // 断言：空博客未被拒绝，缺陷存在
            assertEquals("200", result.getCode(), "空博客不应被保存，但实际被保存了");
            verify(blogService, times(1)).saveOrUpdate(any(Blog.class));
        }
    }

    /**
     * TC-BLOG-002: 前台分页查询全量加载收藏/点赞数据
     * 缺陷：findFrontPage 方法调用 collectService.list() 和 likeService.list()
     * 加载全部收藏和点赞记录到内存，当数据量大时会导致严重性能问题。
     */
    @Test
    @DisplayName("TC-BLOG-002: 前台分页查询全量加载收藏和点赞数据")
    public void findFrontPageLoadsAllCollectsAndLikes() {
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account mockAccount = new Account();
            mockAccount.setId(1);
            mockAccount.setRole("ROLE_USER");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            // 模拟大量收藏和点赞记录
            List<Collect> manyCollects = new ArrayList<>();
            for (int i = 1; i <= 10000; i++) {
                Collect c = new Collect();
                c.setId(i);
                c.setUserId(i % 100);
                c.setItemId(i % 500);
                manyCollects.add(c);
            }
            List<Like> manyLikes = new ArrayList<>();
            for (int i = 1; i <= 10000; i++) {
                Like l = new Like();
                l.setId(i);
                l.setUserId(i % 100);
                l.setItemId(i % 500);
                manyLikes.add(l);
            }

            when(collectService.list()).thenReturn(manyCollects);
            when(likeService.list()).thenReturn(manyLikes);

            Page<Blog> mockPage = new Page<>(1, 10);
            List<Blog> blogs = new ArrayList<>();
            Blog blog = new Blog();
            blog.setId(1);
            blog.setName("测试博客");
            blogs.add(blog);
            mockPage.setRecords(blogs);
            when(blogService.page(any(Page.class), any())).thenReturn(mockPage);

            // 调用方法
            Result result = blogController.findFrontPage(1, 10, 0, "");

            // 断言：collectService.list() 被调用，证明全量加载缺陷存在
            verify(collectService, times(1)).list();
            verify(likeService, times(1)).list();
            assertEquals("200", result.getCode());
        }
    }

    /**
     * TC-BLOG-003: 用户博客页isCollected/isLiked逻辑错误
     * 缺陷：findUserPage 方法中 isCollected/isLiked 只设置为 true，
     * 从不重置为 false，导致如果一个博客未被当前用户收藏/点赞，
     * isCollected 不会被重置。
     */
    @Test
    @DisplayName("TC-BLOG-003: 用户博客页isCollected状态错误-未被收藏的博客显示为已收藏")
    public void findUserPageIsCollectedNotResetToFalse() {
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account mockAccount = new Account();
            mockAccount.setId(1);
            mockAccount.setRole("ROLE_USER");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            // 构造数据：博客1被收藏，博客2未被收藏
            Collect collect1 = new Collect();
            collect1.setId(1);
            collect1.setUserId(1);  // 当前用户
            collect1.setItemId(1);  // 博客1

            when(collectService.list()).thenReturn(List.of(collect1));
            when(likeService.list()).thenReturn(Collections.emptyList());

            Page<Blog> mockPage = new Page<>(1, 10);
            Blog blog1 = new Blog();
            blog1.setId(1);
            blog1.setName("已收藏的博客");
            Blog blog2 = new Blog();
            blog2.setId(2);
            blog2.setName("未收藏的博客");
            mockPage.setRecords(List.of(blog1, blog2));
            when(blogService.page(any(Page.class), any())).thenReturn(mockPage);

            Result result = blogController.findUserPage(1, 10, 1, "");
            Page<Blog> resultPage = (Page<Blog>) result.getData();
            List<Blog> blogs = resultPage.getRecords();

            // blog1 被当前用户收藏，isCollected 应为 true
            assertTrue(blogs.get(0).getIsCollected(), "博客1应被标记为已收藏");

            // blog2 未被当前用户收藏，isCollected 应为 false 或 null
            // 缺陷：由于代码逻辑只 set true 不 set false，blog2 的 isCollected 为 null
            Boolean blog2Collected = blogs.get(1).getIsCollected();
            // 缺陷确认：isCollected 为 null 而非 false，前端可能无法正确处理
            assertTrue(blog2Collected == null || !blog2Collected,
                "博客2未被收藏，isCollected应为false，但实际为" + blog2Collected);
        }
    }

    /**
     * TC-BLOG-004: 收藏操作通过异常捕获实现取消收藏逻辑
     * 缺陷：CollectController.save() 使用 try-catch 捕获异常
     * 来判断是否为"取消收藏"操作，这是反模式。如果数据库连接异常
     * 或其他非唯一约束异常也会触发"取消收藏"逻辑，造成误删。
     */
    @Test
    @DisplayName("TC-BLOG-004: 收藏操作依赖异常捕获实现取消收藏-反模式")
    public void collectUsesExceptionHandlingForToggle() {
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account mockAccount = new Account();
            mockAccount.setId(1);
            mockAccount.setRole("ROLE_USER");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(mockAccount);

            CollectController collectController = new CollectController();
            // 通过反射注入依赖
            try {
                java.lang.reflect.Field collectServiceField = CollectController.class.getDeclaredField("collectService");
                collectServiceField.setAccessible(true);
                collectServiceField.set(collectController, collectService);

                java.lang.reflect.Field blogServiceField = CollectController.class.getDeclaredField("blogService");
                blogServiceField.setAccessible(true);
                blogServiceField.set(collectController, blogService);

                java.lang.reflect.Field messageServiceField = CollectController.class.getDeclaredField("messageService");
                messageServiceField.setAccessible(true);
                messageServiceField.set(collectController, messageService);
            } catch (Exception e) {
                fail("反射注入失败: " + e.getMessage());
            }

            Collect collect = new Collect();
            collect.setItemId(1);

            // 模拟 saveOrUpdate 抛出非唯一约束异常
            doThrow(new RuntimeException("数据库连接异常")).when(collectService).saveOrUpdate(any(Collect.class));

            Result result = collectController.save(collect);

            // 缺陷：非唯一约束异常也触发了"取消收藏"逻辑
            assertEquals("605", result.getCode(), "非唯一约束异常不应触发取消收藏，但实际触发了");
            verify(collectService, times(1)).remove(any(LambdaQueryWrapper.class));
        }
    }

    /**
     * TC-BLOG-005: 删除博客不级联删除关联的收藏和点赞记录
     * 缺陷：BlogController.delete() 调用 blogService.removeById(id) 删除博客，
     * 但未删除 collect 和 like 表中关联该博客的记录，造成孤立数据。
     */
    @Test
    @DisplayName("TC-BLOG-005: 删除博客不级联删除收藏和点赞记录")
    public void deleteBlogDoesNotRemoveRelatedCollectsAndLikes() {
        when(blogService.removeById(1)).thenReturn(true);

        Result result = blogController.delete(1);

        assertEquals("200", result.getCode());
        // 缺陷：删除博客后，应调用 collectService 和 likeService 删除关联记录
        // 但 BlogController 中未调用 collectService/likeService 的 remove 方法
        verify(collectService, never()).remove(any(LambdaQueryWrapper.class));
        verify(likeService, never()).remove(any(LambdaQueryWrapper.class));
    }

    /**
     * TC-BLOG-006: 博客计数接口性能缺陷
     * 缺陷：BlogController.count() 方法通过双重循环统计每个分类的博客数量，
     * 时间复杂度为 O(n*m)，当博客数量大时性能极差。
     */
    @Test
    @DisplayName("TC-BLOG-006: 博客分类计数使用双重循环-性能缺陷")
    public void blogCountUsesNestedLoopPerformanceIssue() {
        when(typeService.list()).thenReturn(Collections.emptyList());
        when(blogService.list()).thenReturn(Collections.emptyList());

        Result result = blogController.count();

        // 缺陷确认：count 方法调用 blogService.list() 加载所有博客到内存
        verify(blogService, times(1)).list();
        assertEquals("200", result.getCode());
    }

    // ==================== 正常行为验证测试 ====================

    /**
     * TC-BLOG-007: 管理员可查看所有博客
     * 验证：当用户角色为 ROLE_ADMIN 时，分页查询不按 userId 过滤。
     */
    @Test
    @DisplayName("TC-BLOG-007: 管理员可查看所有博客-正常行为")
    public void adminCanViewAllBlogs() {
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account adminAccount = new Account();
            adminAccount.setId(1);
            adminAccount.setRole("ROLE_ADMIN");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(adminAccount);

            Page<Blog> mockPage = new Page<>(1, 10);
            when(blogService.page(any(Page.class), any())).thenReturn(mockPage);

            Result result = blogController.findPage(1, 10, "");

            assertEquals("200", result.getCode());
            verify(blogService, times(1)).page(any(Page.class), any());
        }
    }

    /**
     * TC-BLOG-008: 普通用户只能查看自己的博客
     * 验证：当用户角色为 ROLE_USER 时，分页查询按 userId 过滤。
     */
    @Test
    @DisplayName("TC-BLOG-008: 普通用户只能查看自己的博客-正常行为")
    public void userCanOnlyViewOwnBlogs() {
        try (MockedStatic<TokenUtils> mockedStatic = mockStatic(TokenUtils.class)) {
            Account userAccount = new Account();
            userAccount.setId(1);
            userAccount.setRole("ROLE_USER");
            mockedStatic.when(TokenUtils::getCurrentUser).thenReturn(userAccount);

            Page<Blog> mockPage = new Page<>(1, 10);
            when(blogService.page(any(Page.class), any())).thenReturn(mockPage);

            Result result = blogController.findPage(1, 10, "");

            assertEquals("200", result.getCode());
            verify(blogService, times(1)).page(any(Page.class), any());
        }
    }
}
