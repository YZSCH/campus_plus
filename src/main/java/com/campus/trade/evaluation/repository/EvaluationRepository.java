package com.campus.trade.evaluation.repository;

import com.campus.trade.evaluation.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Page<Evaluation> findByTargetUserIdAndStatusOrderByCreatedAtDesc(Long targetUserId, Integer status, Pageable pageable);
    Page<Evaluation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);
    List<Evaluation> findByGoodsIdAndStatusOrderByCreatedAtDesc(Long goodsId, Integer status);
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);
}
