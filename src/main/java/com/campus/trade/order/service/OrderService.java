package com.campus.trade.order.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.repository.GoodsRepository;
import com.campus.trade.message.service.MessageService;
import com.campus.trade.order.dto.OrderDetailVO;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.repository.OrderRepository;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final MessageService messageService;

    public OrderService(OrderRepository or, GoodsRepository gr, UserRepository ur,
                        WalletService ws, MessageService ms) {
        orderRepository = or;
        goodsRepository = gr;
        userRepository = ur;
        walletService = ws;
        messageService = ms;
    }

    public ApiResult<Order> create(Order order) {
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        // 创建时状态为待支付
        order.setStatus("pending");
        order.setPayStatus("unpaid");
        Order saved = orderRepository.save(order);

        // 发送系统通知
        try {
            messageService.notifyBuyer(order.getBuyerId(), "pending", order.getOrderNo(), order.getGoodsId());
            messageService.notifySeller(order.getSellerId(), "pending", order.getOrderNo(), order.getGoodsId());
        } catch (Exception e) {
            // 通知失败不影响订单创建
        }

        return ApiResult.success(saved);
    }

    public ApiResult<Page<Order>> myBuyOrders(Long userId, int page, int size, String status) {
        var p = PageRequest.of(page, size);
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(userId, status, p);
        } else {
            orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId, p);
        }
        orders.forEach(o -> goodsRepository.findById(o.getGoodsId())
                .ifPresent(g -> o.setGoodsTitle(g.getTitle())));
        return ApiResult.success(orders);
    }

    public ApiResult<Page<Order>> mySellOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findBySellerIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        orders.forEach(o -> goodsRepository.findById(o.getGoodsId())
                .ifPresent(g -> o.setGoodsTitle(g.getTitle())));
        return ApiResult.success(orders);
    }

    public ApiResult<Order> updateStatus(Long id, String status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            return ApiResult.success(orderRepository.save(order));
        }).orElse(ApiResult.error(404, "订单不存在"));
    }

    /** 根据 ID 查询订单（供其他模块使用） */
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    /** 根据支付单号查询订单（供支付回调使用） */
    public Order findOrderByPayId(String payId) {
        return orderRepository.findByPayId(payId).orElse(null);
    }

    // ==================== 支付相关 ====================

    /**
     * 更新订单的支付单号（发起支付时调用）
     */
    @Transactional
    public void updatePayId(Long orderId, String payId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setPayId(payId);
            order.setPayStatus("unpaid");
            orderRepository.save(order);
        });
    }

    /**
     * 根据支付单号更新支付状态（异步通知回调时调用）
     *
     * @param payId     支付单号
     * @param payStatus 支付状态：paid
     * @param status    订单状态：paid（待发货）
     */
    @Transactional
    public void updatePayStatus(String payId, String payStatus, String status) {
        orderRepository.findByPayId(payId).ifPresent(order -> {
            // 防止重复更新：只有 unpaid 状态的订单才更新
            if ("unpaid".equals(order.getPayStatus())) {
                order.setPayStatus(payStatus);
                order.setStatus(status);
                orderRepository.save(order);
            }
        });
    }

    // ==================== 订单详情 ====================

    /**
     * 获取订单详情（包含商品、卖家信息和买家信息）
     * 只有买家或卖家可以查看订单详情
     */
    public ApiResult<OrderDetailVO> getOrderDetail(Long orderId, Long userId) {
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ApiResult.error(404, "订单不存在");
        }
        Order order = orderOpt.get();

        // 权限校验：只有买家和卖家可以查看
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return ApiResult.error(403, "无权查看该订单");
        }

        // 查询商品信息
        Goods goods = goodsRepository.findById(order.getGoodsId()).orElse(null);

        // 查询卖家信息
        User seller = userRepository.findById(order.getSellerId()).orElse(null);

        // 查询买家信息
        User buyer = userRepository.findById(order.getBuyerId()).orElse(null);

        OrderDetailVO vo = OrderDetailVO.builder()
                .order(order)
                .goods(goods)
                .seller(seller)
                .buyer(buyer)
                .build();

        return ApiResult.success(vo);
    }

    // ==================== 卖家发货 ====================

    /**
     * 卖家发货：将订单状态从 paid（待发货）改为 shipped（已发货）
     */
    @Transactional
    public ApiResult<Order> shipOrder(Long orderId, Long userId) {
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ApiResult.error(404, "订单不存在");
        }
        Order order = orderOpt.get();

        // 权限校验：只有卖家可以发货
        if (!order.getSellerId().equals(userId)) {
            return ApiResult.error(403, "只有卖家可以发货");
        }
        // 状态校验：只有已支付（待发货）的订单可以发货
        if (!"paid".equals(order.getStatus())) {
            return ApiResult.error(400, "当前订单状态不允许发货");
        }

        order.setStatus("shipped");
        orderRepository.save(order);

        // 发送系统通知
        messageService.notifyBuyer(order.getBuyerId(), "shipped", order.getOrderNo(), order.getGoodsId());
        messageService.notifySeller(order.getSellerId(), "shipped", order.getOrderNo(), order.getGoodsId());

        return ApiResult.success("发货成功", order);
    }

    // ==================== 买家签收 ====================

    /**
     * 买家签收：将订单状态从 shipped（已发货）改为 received（已收货）
     * 同时释放资金给卖家（担保交易完成）
     */
    @Transactional
    public ApiResult<Order> confirmOrder(Long orderId, Long userId) {
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ApiResult.error(404, "订单不存在");
        }
        Order order = orderOpt.get();

        // 权限校验：只有买家可以签收
        if (!order.getBuyerId().equals(userId)) {
            return ApiResult.error(403, "只有买家可以签收");
        }
        // 状态校验：只有已发货的订单可以签收
        if (!"shipped".equals(order.getStatus())) {
            return ApiResult.error(400, "当前订单状态不允许签收");
        }

        order.setStatus("received");
        orderRepository.save(order);

        // 担保交易：订单完成后，释放资金给卖家
        walletService.releaseFunds(orderId);

        // 订单完成后，买家和卖家各增加10信用分
        addCreditScore(order.getBuyerId(), 10);
        addCreditScore(order.getSellerId(), 10);

        // 发送系统通知
        messageService.notifyBuyer(order.getBuyerId(), "received", order.getOrderNo(), order.getGoodsId());
        messageService.notifySeller(order.getSellerId(), "received", order.getOrderNo(), order.getGoodsId());

        return ApiResult.success("签收成功，交易完成", order);
    }

    /**
     * 增加用户信用分
     * @param userId 用户ID
     * @param score 增加的分数
     */
    private void addCreditScore(Long userId, int score) {
        userRepository.findById(userId).ifPresent(user -> {
            int newScore = (user.getCreditScore() == null ? 100 : user.getCreditScore()) + score;
            user.setCreditScore(newScore);
            userRepository.save(user);
        });
    }
}