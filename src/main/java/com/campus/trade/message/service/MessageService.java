package com.campus.trade.message.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.message.entity.Message;
import com.campus.trade.message.repository.MessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public ApiResult<Message> send(Message msg) {
        return ApiResult.success(messageRepository.save(msg));
    }

    /** 发送系统通知 */
    public void sendSystemMessage(Long toUserId, String title, String content, Long goodsId) {
        Message msg = Message.builder()
                .fromUserId(0L) // 0 表示系统消息
                .toUserId(toUserId)
                .content("【" + title + "】" + content)
                .msgType(1) // 1=系统通知
                .isRead(0)
                .goodsId(goodsId)
                .build();
        messageRepository.save(msg);
    }

    /** 发送订单状态通知（买家视角） */
    public void notifyBuyer(Long buyerId, String status, String orderNo, Long goodsId) {
        String content;
        String title;
        switch (status) {
            case "pending":
                content = "订单 " + orderNo + " 已创建，请尽快支付。卖家正在等待您的付款。";
                title = "待付款";
                break;
            case "paid":
                content = "订单 " + orderNo + " 已支付成功，等待卖家发货。资金已安全托管。";
                title = "已付款";
                break;
            case "shipped":
                content = "订单 " + orderNo + " 卖家已发货，请确认收货。收到商品后请点击【签收】完成交易。";
                title = "已发货";
                break;
            case "received":
                content = "订单 " + orderNo + " 交易已完成，感谢您的购买！";
                title = "交易完成";
                break;
            case "cancelled":
                content = "订单 " + orderNo + " 已取消，如有疑问请联系客服。";
                title = "订单取消";
                break;
            default:
                content = "订单 " + orderNo + " 状态更新为：" + status;
                title = "订单状态更新";
        }
        sendSystemMessage(buyerId, title, content, goodsId);
    }

    /** 发送订单状态通知（卖家视角） */
    public void notifySeller(Long sellerId, String status, String orderNo, Long goodsId) {
        String content;
        String title;
        switch (status) {
            case "pending":
                content = "您有新的订单 " + orderNo + "，请等待买家付款。";
                title = "新订单";
                break;
            case "paid":
                content = "订单 " + orderNo + " 买家已付款，请尽快发货。发货后买家将收到通知。";
                title = "已付款";
                break;
            case "shipped":
                content = "订单 " + orderNo + " 已发货，等待买家确认收货。买家签收后款项将自动到账。";
                title = "已发货";
                break;
            case "received":
                content = "订单 " + orderNo + " 交易已完成，款项已到账！感谢您的出售。";
                title = "钱到账";
                break;
            case "cancelled":
                content = "订单 " + orderNo + " 已取消。";
                title = "订单取消";
                break;
            default:
                content = "订单 " + orderNo + " 状态更新为：" + status;
                title = "订单状态更新";
        }
        sendSystemMessage(sellerId, title, content, goodsId);
    }

    public ApiResult<List<Message>> conversation(Long userId1, Long userId2) {
        return ApiResult.success(messageRepository.findConversation(userId1, userId2));
    }

    public ApiResult<Long> unreadCount(Long userId) {
        return ApiResult.success(messageRepository.countUnread(userId));
    }

    /** 标记消息已读 */
    public ApiResult<?> markRead(Long messageId, Long userId) {
        return messageRepository.findById(messageId).map(msg -> {
            if (!msg.getToUserId().equals(userId)) {
                return ApiResult.error(403, "无权操作");
            }
            msg.setIsRead(1);
            return ApiResult.success(messageRepository.save(msg));
        }).orElse(ApiResult.error(404, "消息不存在"));
    }

    /** 获取用户的所有系统通知 */
    public ApiResult<List<Message>> myNotifications(Long userId) {
        return ApiResult.success(messageRepository.findByToUserIdOrderByCreatedAtDesc(userId));
    }

    public ApiResult<List<Long>> getConversationUserIds(Long userId) {
        return ApiResult.success(messageRepository.findConversationUserIds(userId));
    }
}