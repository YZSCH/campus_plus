package com.campus.trade.evaluation.repository;

import com.campus.trade.evaluation.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Page<Evaluation> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
    Page<Evaluation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Evaluation> findByGoodsId(Long goodsId);
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);
}
