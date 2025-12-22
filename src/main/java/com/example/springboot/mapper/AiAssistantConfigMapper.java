package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.entity.AiAssistantConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI助手配置Mapper
 */
@Mapper
public interface AiAssistantConfigMapper extends BaseMapper<AiAssistantConfig> {
}