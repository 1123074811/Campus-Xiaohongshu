package com.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息Mapper接口
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 分页查询两个用户之间的聊天记录
     * @param page 分页参数
     * @param userId1 用户1ID
     * @param userId2 用户2ID
     * @param beforeTime 查询该时间之前的消息，null表示查询最新消息
     * @return 聊天记录列表
     */
    List<ChatMessage> selectChatHistoryByUserIds(Page<ChatMessage> page,
                                                 @Param("userId1") String userId1,
                                                 @Param("userId2") String userId2,
                                                 @Param("beforeTime") String beforeTime);

    /**
     * 标记消息为已读
     * @param toUserId 接收者ID
     * @param fromUserId 发送者ID
     * @return 更新的记录数
     */
    int updateMessageStatusToRead(@Param("toUserId") String toUserId,
                                 @Param("fromUserId") String fromUserId);

    /**
     * 查询用户未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    int countUnreadMessages(@Param("userId") String userId);

    /**
     * 查询用户与某个特定好友的未读消息数量
     * @param toUserId 接收者ID
     * @param fromUserId 发送者ID
     * @return 未读消息数量
     */
    int countUnreadMessagesFromUser(@Param("toUserId") String toUserId,
                                  @Param("fromUserId") String fromUserId);
}
