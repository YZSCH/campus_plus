package com.campus.trade.pay.dto;

import lombok.Data;

/**
 * 退款请求 DTO
 */
@Data
public class RefundRequest {
    /** 订单ID（本系统订单ID，用于更新订单状态） */
    private Long orderId;
    /** 支付宝支付单号 */
    private String payId;
    /** 退款金额（元，字符串格式） */
    private String value;
    /** 退款原因 */
    private String reason;
}
