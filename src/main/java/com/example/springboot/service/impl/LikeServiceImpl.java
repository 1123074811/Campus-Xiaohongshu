package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.Like;
import com.example.springboot.mapper.LikeMapper;
import com.example.springboot.service.ILikeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements ILikeService {

    @Resource
    private LikeMapper likeMapper;

}
