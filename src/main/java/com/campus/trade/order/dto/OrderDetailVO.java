package com.campus.trade.order.dto;

import com.campus.trade.goods.entity.Goods;
import com.campus.trade.order.entity.Order;
import com.campus.trade.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单详情 VO
 * 包含：订单信息 + 商品信息 + 卖家信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailVO {

    /** 订单信息 */
    private Order order;

    /** 商品信息 */
    private Goods goods;

    /** 卖家信息 */
    private User seller;
}
