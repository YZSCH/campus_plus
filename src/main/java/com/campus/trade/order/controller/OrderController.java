package com.campus.trade.order.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.order.dto.OrderDetailVO;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService os, UserRepository ur) {
        orderService = os;
        userRepository = ur;
    }

    @PostMapping("/create")
    public ApiResult<Order> create(@RequestBody Order order, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "请先登录");
        Long userId = (Long) auth.getPrincipal();
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) return ApiResult.error(401, "用户不存在");
        if (user.getCreditScore() == null || user.getCreditScore() < 60)
            return ApiResult.error(403, "信用分低于60分，无法购买商品");
        order.setBuyerId(userId);
        return orderService.create(order);
    }

    @GetMapping("/buy")
    public ApiResult<Page<Order>> buyOrders(Authentication auth,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) String status) {
        return orderService.myBuyOrders((Long) auth.getPrincipal(), page, size, status);
    }

    @GetMapping("/sell")
    public ApiResult<Page<Order>> sellOrders(Authentication auth,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return orderService.mySellOrders((Long) auth.getPrincipal(), page, size);
    }

    @PutMapping("/{id}/status")
    public ApiResult<Order> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.updateStatus(id, status);
    }

    // ==================== 新增接口 ====================

    /**
     * 订单详情
     * 返回：订单信息 + 商品信息 + 卖家信息
     *
     * 前端根据订单状态显示对应按钮：
     *   - status=pending & payStatus=unpaid → 显示「去支付」
     *   - status=paid（待发货）→ 买家显示「等待卖家发货」，卖家显示「发货」
     *   - status=shipped（已发货）→ 买家显示「签收」，卖家显示「已发货」
     *   - status=received（已收货）→ 显示「交易完成」
     */
    @GetMapping("/{id}/detail")
    public ApiResult<OrderDetailVO> getOrderDetail(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderDetail(id, (Long) auth.getPrincipal());
    }

    /**
     * 卖家发货
     * 状态变更：paid（待发货）→ shipped（已发货）
     */
    @PutMapping("/{id}/ship")
    public ApiResult<Order> shipOrder(@PathVariable Long id, Authentication auth) {
        return orderService.shipOrder(id, (Long) auth.getPrincipal());
    }

    /**
     * 买家签收
     * 状态变更：shipped（已发货）→ received（已收货）
     */
    @PutMapping("/{id}/confirm")
    public ApiResult<Order> confirmOrder(@PathVariable Long id, Authentication auth) {
        return orderService.confirmOrder(id, (Long) auth.getPrincipal());
    }
}