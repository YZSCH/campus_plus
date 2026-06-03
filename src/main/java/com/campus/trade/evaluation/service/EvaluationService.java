package com.campus.trade.evaluation.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.evaluation.entity.Evaluation;
import com.campus.trade.evaluation.repository.EvaluationRepository;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final OrderRepository orderRepository;
    public EvaluationService(EvaluationRepository er, OrderRepository or) { evaluationRepository = er; orderRepository = or; }

    public ApiResult<Evaluation> create(Evaluation eval) {
        if (eval.getOrderId() == null) return ApiResult.error(400, "请选择对应完成订单");
        if (eval.getRating() == null || eval.getRating() < 1 || eval.getRating() > 5) return ApiResult.error(400, "评分必须在1到5分之间");
        if (eval.getContent() == null || eval.getContent().trim().isEmpty()) return ApiResult.error(400, "评价内容不能为空");
        if (eval.getContent().length() > 500) return ApiResult.error(400, "评价内容不能超过500字");
        Order order = orderRepository.findById(eval.getOrderId()).orElse(null);
        if (order == null) return ApiResult.error(404, "订单不存在");
        if (!"received".equals(order.getStatus())) return ApiResult.error(400, "订单完成后才能评价");
        if (!order.getBuyerId().equals(eval.getUserId()) && !order.getSellerId().equals(eval.getUserId())) return ApiResult.error(403, "无权评价该订单");
        Long targetUserId = order.getBuyerId().equals(eval.getUserId()) ? order.getSellerId() : order.getBuyerId();
        eval.setTargetUserId(targetUserId);
        eval.setGoodsId(order.getGoodsId());
        eval.setStatus(0);
        eval.setReportReason(null);
        eval.setReportedAt(null);
        if (evaluationRepository.existsByOrderIdAndUserId(eval.getOrderId(), eval.getUserId()))
            return ApiResult.error(400, "已评价过该订单");
        return ApiResult.success(evaluationRepository.save(eval));
    }

    public ApiResult<Page<Evaluation>> listByUser(Long userId, int page, int size) {
        return ApiResult.success(evaluationRepository.findByTargetUserIdAndStatusOrderByCreatedAtDesc(userId, 0, PageRequest.of(page, size)));
    }

    public ApiResult<List<Evaluation>> listByGoods(Long goodsId) {
        return ApiResult.success(evaluationRepository.findByGoodsIdAndStatusOrderByCreatedAtDesc(goodsId, 0));
    }

    public ApiResult<Page<Evaluation>> listMine(Long userId, int page, int size) {
        return ApiResult.success(evaluationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, 0, PageRequest.of(page, size)));
    }

    public ApiResult<String> delete(Long id, Long userId) {
        return evaluationRepository.findById(id).map(eval -> {
            if (!eval.getUserId().equals(userId)) return ApiResult.<String>error(403, "无权删除该评价");
            evaluationRepository.delete(eval);
            return ApiResult.success("删除成功", "删除成功");
        }).orElse(ApiResult.error(404, "评价不存在"));
    }

    public ApiResult<String> report(Long id, Long userId, String reason) {
        return evaluationRepository.findById(id).map(eval -> {
            if (eval.getUserId().equals(userId)) return ApiResult.<String>error(400, "不能举报自己的评价");
            if (reason == null || reason.trim().isEmpty()) return ApiResult.<String>error(400, "请填写举报原因");
            if (reason.length() > 500) return ApiResult.<String>error(400, "举报原因不能超过500字");
            eval.setStatus(1);
            eval.setReportReason(reason.trim());
            eval.setReportedAt(LocalDateTime.now());
            evaluationRepository.save(eval);
            return ApiResult.success("举报成功，平台将尽快处理", "举报成功");
        }).orElse(ApiResult.error(404, "评价不存在"));
    }

    public ApiResult<Double> getAverageRating(Long targetUserId) {
        Double avg = evaluationRepository.getAverageRatingByTargetUserIdAndStatus(targetUserId, 0);
        return ApiResult.success(avg);
    }
}
