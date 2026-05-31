package com.campus.trade.pay.dto;

import lombok.Data;

/**
 * 支付请求 DTO
 */
@Data
public class PayRequest {
    /** 订单ID */
    private Long orderId;
}
