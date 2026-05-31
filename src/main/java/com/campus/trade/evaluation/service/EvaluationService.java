package com.campus.trade.evaluation.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.evaluation.entity.Evaluation;
import com.campus.trade.evaluation.repository.EvaluationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    public EvaluationService(EvaluationRepository er) { evaluationRepository = er; }

    public ApiResult<Evaluation> create(Evaluation eval) {
        if (evaluationRepository.existsByOrderIdAndUserId(eval.getOrderId(), eval.getUserId()))
            return ApiResult.error(400, "已评价过该订单");
        return ApiResult.success(evaluationRepository.save(eval));
    }

    public ApiResult<Page<Evaluation>> listByUser(Long userId, int page, int size) {
        return ApiResult.success(evaluationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)));
    }

    public ApiResult<List<Evaluation>> listByGoods(Long goodsId) {
        return ApiResult.success(evaluationRepository.findByGoodsId(goodsId));
    }
}
