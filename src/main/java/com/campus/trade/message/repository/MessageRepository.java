package com.campus.trade.message.repository;

import com.campus.trade.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 查询两个用户之间的私信对话 */
    @Query("SELECT m FROM Message m WHERE (m.fromUserId = :u1 AND m.toUserId = :u2) OR (m.fromUserId = :u2 AND m.toUserId = :u1) ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("u1") Long u1, @Param("u2") Long u2);

    /** 统计用户未读消息数 */
    Long countByToUserIdAndIsRead(Long userId, Integer isRead);

    /** 默认方法：统计未读消息（isRead=0） */
    default Long countUnread(Long userId) {
        return countByToUserIdAndIsRead(userId, 0);
    }

    /** 查询用户收到的所有通知（按时间倒序） */
    List<Message> findByToUserIdOrderByCreatedAtDesc(Long toUserId);

    /** 分页查询用户通知 */
    Page<Message> findByToUserIdOrderByCreatedAtDesc(Long toUserId, Pageable pageable);
}